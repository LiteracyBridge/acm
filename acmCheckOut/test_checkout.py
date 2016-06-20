"""
test_checkout.py

Test suite for basic functionality of checkout.py lambda function.
Tests all use-cases for the six available transaction types:
    "checkIn" "checkOut" "revokeCheckOut" "discard" "statusCheck" "new" (subset of checkIn)

"""
import unittest
import boto3
import json
from datetime import datetime
from random import randint
from setupDB import create_DB


# # creates a new table to use for testing: "test_acm_check_out"
# def setUpModule():
#
#     create_DB('test_acm_check_out')
#     time.sleep(2)    # sleep required to give AWS time to create DB
#
#
# # deletes "test_acm_check_out" table
# def tearDownModule():
#     dynamodb = boto3.resource('dynamodb', region_name='us-west-2')
#     table = dynamodb.Table('test_acm_check_out')
#     status = table.table_status
#     while status != "ACTIVE":
#         status = table.table_status
#     delete_response = table.delete()
#     print "Deleted table: ", "test_acm_check_out"

#specify table to use for testing
TABLE = 'acm_check_out'

_lambda = boto3.client("lambda")
dynamodb = boto3.resource('dynamodb', region_name='us-west-2')
table = dynamodb.Table(TABLE)

# helper function to invoke lambda function with different payloads & return response payload
def invoke(_lambda, payload):
    response = _lambda.invoke(
        FunctionName="acmCheckOut",
        Payload=json.dumps(payload)
    )
    return response['Payload'].read()


# helper function to create checked-in ACMs
def newCheckedInACM(acm_name, user_name):
    filename = "db"+str(randint(0, 100))+".zip"
    contact = str(randint(1000000000,9999999999))
    transaction_date = str(datetime.now())
    update_response = table.update_item(
        Key={
            'acm_name': acm_name,
        },
        UpdateExpression='SET acm_state = :t, last_in_file_name = :f, last_in_name = :n, last_in_contact = :c, last_in_date = :d',
        ExpressionAttributeValues={
            ':t': "CHECKED_IN",
            ':f': filename,
            ':n': user_name,
            ':c': contact,
            ':d': transaction_date,
        })

# helper function to create checked-out ACMs
def newCheckedOutACM(acm_name, user_name, new_key):
    version = "r"+str(randint(100000,999999))+"n"
    #new_key = str(randint(10000000000))
    contact = str(randint(1000000000,9999999999))
    transaction_date = str(datetime.now())
    update_response = table.update_item(
        Key={
            'acm_name': acm_name,
        },
        UpdateExpression='SET acm_state = :t, now_out_name = :n, now_out_contact = :c, now_out_key = :k, now_out_version = :v, now_out_date = :d, now_out_comment = :o',
        ExpressionAttributeValues={
            ':t': "CHECKED_OUT",
            ':n': user_name,
            ':c': contact,
            ':k': str(new_key),
            ':v': version,
            ':d': transaction_date,
            ':o': "TEST"
        })

# helper function to delete ACMs between tests
def deleteACM(acm_name,transaction):
    table.update_item(
        Key={
            'acm_name':acm_name
        },
        UpdateExpression='REMOVE now_out_name, now_out_contact, now_out_date, now_out_key, now_out_comment SET acm_state = :s',
        ExpressionAttributeValues={
            ':s': 'CHECKED_IN'
        })


class CheckInTests(unittest.TestCase):
    # sets up ACMs to use for testing
    @classmethod
    def setUpClass(cls):
        if table.table_status == "ACTIVE":
            newCheckedInACM("checked_in_test_acm", "Nihala CheckedIn")
            newCheckedInACM("checked_out_test_acm","Nihala CheckedOut")
            newCheckedOutACM("checked_out_test_acm","Nihala CheckedOut", str(12345))
    @classmethod
    def tearDownClass(cls):
        if table.table_status == "ACTIVE":
            table.delete_item(
            Key={
                'acm_name': "make_a_new_entry",
            })
            table.delete_item(
            Key={
                'acm_name': "make_a_new_entry_1023981294397495731203129312949857283213012",
            })

    # makes sure there is a checked-out & checked-in ACM to use for testing
    def setUp(self):
        if table.table_status == "ACTIVE":
            newCheckedOutACM("checked_out_test_acm","Nihala CheckedOut", str(12345))
            deleteACM("checked_in_test_acm","CHECK_OUT")



    def test_standardCheckIn_ok(self):
        # try to check-in an item that is checked out to me with matching key
        payload = {
            "db": "checked_out_test_acm",
            "action": "checkIn",
            "key": "12345",
            "filename": "test1.zip",
            "name": "Nihala CheckedOut",
            "contact": "9789305190",
            "version": "r1606240",
            "computername": "nihs_laptop"
        }
        response = invoke(_lambda, payload)
        print "test_standardCheckIn_ok"
        self.assertIn('"data": "ok"', response)

    def test_standardCheckIn_ok2(self):
        # try to check-in an item that is checked out to me with matching key but missing non-vital post parameters
        payload = {
            "db": "checked_out_test_acm",
            "action": "checkIn",
            "key": "12345",
            "filename": "test2.zip",
            "name": "Nihala CheckedOut",
        }
        response = invoke(_lambda, payload)
        print "test_standardCheckIn_ok2"
        self.assertIn('"data": "ok"', response)

    def test_standardCheckIn_ok3(self):
        # try to check-in an item that is checked out to me with matching key but long contact number
        payload = {
            "db": "checked_out_test_acm",
            "action": "checkIn",
            "key": "12345",
            "filename": "test3.zip",
            "name": "Nihala CheckedOut",
            "contact": "9789305138124791374819273982391237189237129421375621283213090",
            "version": "r1606240",
            "computername": "nihs_laptop"
        }
        response = invoke(_lambda, payload)
        print "test_standardCheckIn_ok3"
        self.assertIn('"data": "ok"', response)

    def test_standardCheckIn_fail1(self):
        # try to check-in an item that is checked out to someone else but with correct key
        payload = {
            "db": "checked_out_test_acm",
            "action": "checkIn",
            "key": "12345",
            "filename": "test4.zip",
            "name": "Nihala Somebody",
            "contact": "9789305190",
            "version": "r1606240",
            "computername": "nihs_laptop"
        }
        response = invoke(_lambda, payload)
        print "test_standardCheckIn_fail1"
        self.assertIn('"data": "denied"', response)
        self.assertNotIn('error',response)

    def test_standardCheckIn_fail2(self):
        # try to check-in an item that is checked out to me but without correct key
        payload = {
            "db": "checked_out_test_acm",
            "action": "checkIn",
            "key": "1234235",
            "filename": "test5.zip",
            "name": "Nihala CheckedOut",
            "contact": "9789305190",
            "version": "r1606240",
            "computername": "nihs_laptop"
        }
        response = invoke(_lambda, payload)
        print "test_standardCheckIn_fail2"
        self.assertIn('"data": "denied"', response)
        self.assertNotIn('error', response)

    def test_standardCheckIn_fail3(self):
        # try to check-in an item that is not checked out to me and without correct key
        payload = {
            "db": "checked_out_test_acm",
            "action": "checkIn",
            "key": "123423215",
            "filename": "test6.zip",
            "name": "Nihala Somebody",
            "contact": "9789305190",
            "version": "r1606240",
            "computername": "nihs_laptop"
        }
        response = invoke(_lambda, payload)
        print "test_standardCheckIn_fail3"
        self.assertIn('"data": "denied"', response)
        self.assertNotIn('error', response)

    def test_standardCheckIn_fail4(self):
        # try to check-in an item that is checked out to me but missing vital post parameter (key)
        payload = {
            "db": "checked_out_test_acm",
            "action": "checkIn",
            "filename": "test7.zip",
            "name": "Nihala CheckedOut",
            "contact": "9789305190",
            "version": "r1606240",
            "computername": "nihs_laptop"
        }
        response = invoke(_lambda, payload)
        print "test_standardCheckIn_fail4"
        self.assertIn('"data": "denied"', response)
        self.assertNotIn('error', response)

    def test_standardCheckIn_fail5(self):
        # try to check-in an item that is checked out with matching key but missing vital post parameter (name)
        payload = {
            "db": "checked_out_test_acm",
            "action": "checkIn",
            "key": "12345",
            "filename": "test8.zip",
            "contact": "9789305190",
            "version": "r1606240",
            "computername": "nihs_laptop"
        }
        response = invoke(_lambda, payload)
        print "test_standardCheckIn_fail5"
        self.assertIn('"data": "denied"', response)
        self.assertNotIn('error', response)

    def test_standardCheckIn_fail6(self):
        # try to check-in an item that is checked out to me with matching key but missing vital post parameter (action)
        payload = {
            "db": "checked_out_test_acm",
            "key": "12345",
            "filename": "test9.zip",
            "name": "Nihala CheckedOut",
            "contact": "9789305190",
            "version": "r1606240",
            "computername": "nihs_laptop"
        }
        response = invoke(_lambda, payload)
        print "test_standardCheckIn_fail6"
        self.assertIn('"data": "denied"', response)
        self.assertNotIn('error', response)

    def test_standardCheckIn_fail7(self):
        # try to check-in an item that does not exist
        payload = {
            "db": "this_is_not_an_acm",
            "action": "checkIn",
            "key": "12345",
            "filename": "test10.zip",
            "name": "Nihala CheckedOut",
            "contact": "9789305190",
            "version": "r1606240",
            "computername": "nihs_laptop"
        }
        response = invoke(_lambda, payload)
        print "test_standardCheckIn_fail7"
        self.assertIn('"data": "denied"', response)
        self.assertNotIn('error', response)

    def test_standardCheckIn_fail8(self):
        # try to check-in an item that is not checked-out
        payload = {
            "db": "checked_in_test_acm",
            "action": "checkIn",
            "key": "123423215",
            "filename": "test11.zip",
            "name": "Nihala CheckedOut",
            "contact": "9789305190",
            "version": "r1606240",
            "computername": "nihs_laptop"
        }
        response = invoke(_lambda, payload)
        print "test_standardCheckIn_fail8"
        self.assertIn('"data": "denied"', response)
        self.assertNotIn('error', response)

    def test_newCheckIn_ok1(self):
        # check-in an item that does not exist with key = "new"
        payload = {
            "db": "make_a_new_entry",
            "action": "checkIn",
            "key": "new",
            "filename": "test12.zip",
            "name": "ThisIsMe",
            "contact": "9789305190",
            "version": "r1606240",
            "computername": "nihs_laptop"
        }
        response = invoke(_lambda, payload)
        print "test_newCheckIn_ok1"
        self.assertIn('"data": "ok"', response)

    def test_newCheckIn_ok2(self):
        # check-in an item that does not exist with key = "new" that has a long name
        payload = {
            "db": "make_a_new_entry_1023981294397495731203129312949857283213012",
            "action": "checkIn",
            "key": "new",
            "filename": "test13.zip",
            "name": "ThisIsMe",
            "contact": "9789305190",
            "version": "r1606240",
            "computername": "nihs_laptop"
        }
        response = invoke(_lambda, payload)
        print "test_newCheckIn_ok2"
        self.assertIn('"data": "ok"', response)

    def test_newCheckIn_fail1(self):
        # check-in an item that is checked-out by me with key = "new"
        payload = {
            "db": "checked_out_test_acm",
            "action": "checkIn",
            "key": "new",
            "filename": "test14.zip",
            "name": "Nihala CheckedOut",
            "contact": "9789305190",
            "version": "r1606240",
            "computername": "nihs_laptop"
        }
        response = invoke(_lambda, payload)
        print "test_newCheckIn_fail1"
        self.assertIn('"data": "denied"', response)
        self.assertNotIn('error', response)

    def test_newCheckIn_fail2(self):
        # check-in an item that is checked-out by someone else with key = "new"
        payload = {
            "db": "checked_out_test_acm",
            "action": "checkIn",
            "key": "new",
            "filename": "test15.zip",
            "name": "Who Am I",
            "contact": "9789305190",
            "version": "r1606240",
            "computername": "nihs_laptop"
        }
        response = invoke(_lambda, payload)
        print "test_newCheckIn_fail2"
        self.assertIn('"data": "denied"', response)
        self.assertNotIn('error', response)

    def test_newCheckIn_fail3(self):
        # check-in an item that is already checked-in with key = "new"
        payload = {
            "db": "checked_in_test_acm",
            "action": "checkIn",
            "key": "new",
            "filename": "test16.zip",
            "name": "My name doesn't matter",
            "contact": "9789305190",
            "version": "r1606240",
            "computername": "nihs_laptop"
        }
        response = invoke(_lambda, payload)
        print "test_newCheckIn_fail3"
        self.assertIn('"data": "denied"', response)
        self.assertNotIn('error', response)

    def test_discard_ok(self):
        # try to discard an item that is checked out to me with matching key
        payload = {
            "db": "checked_out_test_acm",
            "action": "discard",
            "key": "12345",
            "filename": "test17.zip",
            "name": "Nihala CheckedOut",
            "contact": "9789305190",
            "version": "r1606240",
            "computername": "nihs_laptop"
        }
        response = invoke(_lambda, payload)
        print "test_discard_ok"
        self.assertIn('"data": "ok"', response)

    def test_discard_ok2(self):
        # try to discard an item that is checked out to me with matching key but missing non-vital post parameters
        payload = {
            "db": "checked_out_test_acm",
            "action": "discard",
            "key": "12345",
            "filename": "test18.zip",
            "name": "Nihala CheckedOut",
        }
        response = invoke(_lambda, payload)
        print "test_discard_ok2"
        self.assertIn('"data": "ok"', response)

    def test_discard_ok3(self):
        # try to discard an item that is checked out to me with matching key but long contact number
        payload = {
            "db": "checked_out_test_acm",
            "action": "checkIn",
            "key": "12345",
            "filename": "test19.zip",
            "name": "Nihala CheckedOut",
            "contact": "9789305138124791374819273982391237189237129421375621283213090",
            "version": "r1606240",
            "computername": "nihs_laptop"
        }
        response = invoke(_lambda, payload)
        print "test_discard_ok3"
        self.assertIn('"data": "ok"', response)

    def test_discard_ok4(self):
        # try to discard an item that is not checked-out
        payload = {
            "db": "checked_in_test_acm",
            "action": "discard",
            "key": "123423215",
            "filename": "test20.zip",
            "name": "Nihala CheckedOut",
            "contact": "9789305190",
            "version": "r1606240",
            "computername": "nihs_laptop"
        }
        response = invoke(_lambda, payload)
        print "test_discard_ok4"
        self.assertIn('"data": "ok"', response)
        self.assertNotIn('error', response)

    def test_discard_fail1(self):
        # try to discard an item that is checked out to someone else but with correct key
        payload = {
            "db": "checked_out_test_acm",
            "action": "discard",
            "key": "12345",
            "filename": "test21.zip",
            "name": "Nihala Somebody",
            "contact": "9789305190",
            "version": "r1606240",
            "computername": "nihs_laptop"
        }
        response = invoke(_lambda, payload)
        print "test_standardCheckIn_fail1"
        self.assertIn('"data": "denied"', response)
        self.assertNotIn('error', response)

    def test_discard_fail2(self):
        # try to discard an item that is checked out to me but without correct key
        payload = {
            "db": "checked_out_test_acm",
            "action": "discard",
            "key": "1234235",
            "filename": "test22.zip",
            "name": "Nihala CheckedOut",
            "contact": "9789305190",
            "version": "r1606240",
            "computername": "nihs_laptop"
        }
        response = invoke(_lambda, payload)
        print "test_discard_fail2"
        self.assertIn('"data": "denied"', response)
        self.assertNotIn('error', response)

    def test_discard_fail3(self):
        # try to discard an item that is not checked out to me and without correct key
        payload = {
            "db": "checked_out_test_acm",
            "action": "discard",
            "key": "123423215",
            "filename": "test23.zip",
            "name": "Nihala Somebody",
            "contact": "9789305190",
            "version": "r1606240",
            "computername": "nihs_laptop"
        }
        response = invoke(_lambda, payload)
        print "test_discard_fail3"
        self.assertIn('"data": "denied"', response)
        self.assertNotIn('error', response)

    def test_discard_fail4(self):
        # try to discard an item that is checked out to me but missing vital post parameter (key)
        payload = {
            "db": "checked_out_test_acm",
            "action": "discard",
            "filename": "test24.zip",
            "name": "Nihala CheckedOut",
            "contact": "9789305190",
            "version": "r1606240",
            "computername": "nihs_laptop"
        }
        response = invoke(_lambda, payload)
        print "test_discard_fail4"
        self.assertIn('"data": "denied"', response)
        self.assertNotIn('error', response)

    def test_discard_fail5(self):
        # try to discard an item that is checked out with matching key but missing vital post parameter (name)
        payload = {
            "db": "checked_out_test_acm",
            "action": "discard",
            "key": "12345",
            "filename": "test25.zip",
            "contact": "9789305190",
            "version": "r1606240",
            "computername": "nihs_laptop"
        }
        response = invoke(_lambda, payload)
        print "test_discard_fail5"
        self.assertIn('"data": "denied"', response)
        self.assertNotIn('error', response)

    def test_discard_fail6(self):
        # try to discard an item that does not exist
        payload = {
            "db": "this_is_not_an_acm",
            "action": "checkIn",
            "key": "12345",
            "filename": "test26.zip",
            "name": "Nihala CheckedOut",
            "contact": "9789305190",
            "version": "r1606240",
            "computername": "nihs_laptop"
        }
        response = invoke(_lambda, payload)
        print "test_discard_fail6"
        self.assertIn('"data": "denied"', response)
        self.assertNotIn('error', response)

    def test_standardCheckOut_ok1(self):
        # try to check-out an item that is checked-in
        payload = {
            "db": "checked_in_test_acm",
            "action": "checkOut",
            "name": "Nihala CheckedOut",
            "contact": "9789305190",
            "version": "r1606240",
            "computername": "nihs_laptop"
        }
        response = invoke(_lambda, payload)
        print "test_standardCheckOut_ok1"
        self.assertIn("key", response)
        self.assertIn("filename",response)
        self.assertNotIn('error', response)

    def test_standardCheckOut_ok2(self):
        # try to check-out an item that is checked-in but missing post parameter (name)
        payload = {
            "db": "checked_in_test_acm",
            "action": "checkOut",
            "contact": "9789305190",
            "version": "r1606240",
            "computername": "nihs_laptop"
        }
        response = invoke(_lambda, payload)
        print "test_standardCheckOut_ok2"
        self.assertIn("key", response)
        self.assertIn("filename",response)
        self.assertNotIn('error', response)

    def test_standardCheckOut_ok3(self):
        # try to check-out an item that is checked-in but missing post parameter (contact)
        payload = {
            "db": "checked_in_test_acm",
            "action": "checkOut",
            "name": "Nihala CheckedOut",
            "version": "r1606240",
            "computername": "nihs_laptop"
        }
        response = invoke(_lambda, payload)
        print "test_standardCheckOut_ok3"
        self.assertIn("key", response)
        self.assertIn("filename",response)
        self.assertNotIn('error', response)

    def test_standardCheckOut_fail1(self):
        # try to check-out an item that does not exist
        payload = {
            "db": "i_made_up_this_acm",
            "action": "checkOut",
            "name": "Nihala CheckedOut",
            "contact": "9789305190",
            "version": "r1606240",
            "computername": "nihs_laptop"
        }
        response = invoke(_lambda, payload)
        print "test_standardCheckOut_fail1"
        self.assertIn("filename=NULL",response)
        self.assertNotIn('error', response)

    def test_standardCheckOut_fail2(self):
        # try to check-out an item that is already checked-out
        payload = {
            "db": "checked_out_test_acm",
            "action": "checkOut",
            "name": "Nihala CheckedOut",
            "contact": "9789305190",
            "version": "r1606240",
            "computername": "nihs_laptop"
        }
        response = invoke(_lambda, payload)
        print "test_standardCheckOut_fail1"
        self.assertIn('possessor',response)
        self.assertIn('filename',response)
        self.assertNotIn('error', response)

    def test_revokeCheckOut_ok1(self):
        # revoke a check-out from myself
        payload = {
            "db": "checked_out_test_acm",
            "action": "revokeCheckOut",
            "name": "Nihala CheckedOut",
            "contact": "9789305190",
            "version": "r1606240",
            "computername": "nihs_laptop"
        }
        response = invoke(_lambda, payload)
        print "test_revokeCheckOut_ok1"
        self.assertIn('"data": "ok"', response)
        self.assertNotIn('error', response)

    def test_revokeCheckOut_ok2(self):
        # revoke a check-out from someone else
        payload = {
            "db": "checked_out_test_acm",
            "action": "revokeCheckOut",
            "name": "This is Not My Check Out",
            "contact": "9789305190",
            "version": "r1606240",
            "computername": "nihs_laptop"
        }
        response = invoke(_lambda, payload)
        print "test_revokeCheckOut_ok2"
        self.assertIn('"data": "ok"', response)
        self.assertNotIn('error', response)

    def test_revokeCheckOut_ok3(self):
        # revoke a check-out that does not exist
        payload = {
            "db": "who_knows_what_this_is_its_not_real",
            "action": "revokeCheckOut",
            "name": "My name doesnt matter",
            "contact": "9789305190",
            "version": "r1606240",
            "computername": "nihs_laptop"
        }
        response = invoke(_lambda, payload)
        print "test_revokeCheckOut_ok3"
        self.assertIn('"data": "ok"', response)
        self.assertNotIn('error', response)

    def test_revokeCheckOut_ok4(self):
        # revoke a check-out that is checked-in
        payload = {
            "db": "checked_in_test_acm",
            "action": "revokeCheckOut",
            "name": "My name doesn't matter",
            "contact": "9789305190",
            "version": "r1606240",
            "computername": "nihs_laptop"
        }
        response = invoke(_lambda, payload)
        print "test_revokeCheckOut_ok4"
        self.assertIn('"data": "ok"', response)
        self.assertNotIn('error', response)

    def test_statusCheck_ok1(self):
        # check the status of an ACM that does not exist
        payload = {
            "db": "abc_def_this_is_no_acm",
            "action": "statusCheck",
            "name": "My name doesn't matter",
            "contact": "9789305190",
            "version": "r1606240",
            "computername": "nihs_laptop"
        }
        response = invoke(_lambda, payload)
        print "test_statusCheck_ok1"
        self.assertIn('filename=NULL', response)
        self.assertNotIn('error', response)

    def test_statusCheck_ok2(self):
        # check the status of an ACM that is checked-out to me
        payload = {
            "db": "checked_out_test_acm",
            "action": "statusCheck",
            "name": "Nihala CheckedOut",
            "contact": "9789305190",
            "version": "r1606240",
            "computername": "nihs_laptop"
        }
        response = invoke(_lambda, payload)
        print "test_statusCheck_ok2"
        self.assertIn('possessor=Nihala CheckedOut', response)
        self.assertIn('filename=', response)
        self.assertNotIn('error', response)

    def test_statusCheck_ok3(self):
        # check the status of an ACM that is checked-out to someone else
        payload = {
            "db": "checked_out_test_acm",
            "action": "statusCheck",
            "name": "Simba Samba",
            "contact": "9789305190",
            "version": "r1606240",
            "computername": "nihs_laptop"
        }
        response = invoke(_lambda, payload)
        print "test_statusCheck_ok2"
        self.assertIn('possessor', response)
        self.assertIn('filename', response)
        self.assertNotIn('error', response)

    def test_statusCheck_ok4(self):
        # check the status of an ACM that is available (i.e. checked-in)
        payload = {
            "db": "checked_in_test_acm",
            "action": "statusCheck",
            "name": "Simba Samba",
            "contact": "9789305190",
            "version": "r1606240",
            "computername": "nihs_laptop"
        }
        response = invoke(_lambda, payload)
        print "test_statusCheck_ok2"
        self.assertIn('filename', response)
        self.assertNotIn('possessor',response)
        self.assertNotIn('error', response)


if __name__ == "__main__":
    unittest.main()

