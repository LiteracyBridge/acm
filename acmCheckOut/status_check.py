"""
status_check.py

A CLI script that allows a user to query the status of ACMs in the database.

To run, in the command line type    'python status_check.py -m MODE'    with one of the following modes:

1. all: list all ACM's and their statuses
2. out: list checked-out ACMs and who has them
3. in: list all checked-in ACMs
"""
import boto3
import argparse

TABLE = 'test_acm_check_out'    # back-end dynamoDB table


def status_check(mode='all'):
    """
    Check status of ACMs in the database (see above)

    :param mode: string -- "all", "out", "in"   default mode: all
    :return: prints out a list of ACMs
    """
    try:
        dynamodb = boto3.resource('dynamodb', region_name='us-west-2')
        table = dynamodb.Table(TABLE)
        # scan all table entries and return a dict containing items with specified attributes
        response = table.scan(
            TableName=TABLE,
            AttributesToGet=[
                'acm_name',
                'transaction',
                'user_name'
            ],
            Select='SPECIFIC_ATTRIBUTES',
        )
        items = response['Items']
    except Exception as err:
        print err

    if items:
        if mode == "all":
            return list_all(items)
        if mode == "out":
            return list_out(items)
        if mode == "in":
            return list_in(items)

# helper function for listing status of all ACMs
def list_all(items):
    print "---------------------------------------------------------------"
    print "ACM Status Check"
    print "---------------------------------------------------------------"
    acm_out = []    # keep track of checked out ACMs
    for item in items:
        if item["transaction"]=="CHECK_OUT":
            acm_out.append(item["acm_name"])
            print "Out" + "....." + str(item["acm_name"])
    for item in items:
        if item["acm_name"] not in acm_out:
            print "In" + " ....." + str(item["acm_name"])
    return

# helper function to list checked-out ACMs
def list_out(items):
    print "---------------------------------------------------------------"
    print "Checked-Out ACMs"
    print "---------------------------------------------------------------"
    for item in items:
        if item["transaction"]=="CHECK_OUT":
            print str(item["acm_name"]) + "....."+ str(item["user_name"])
    return

# helper function to list checked-in ACMs
def list_in(items):
    print "---------------------------------------------------------------"
    print "Checked-In ACMs"
    print "---------------------------------------------------------------"
    acm_out = []  # keep track of checked out ACMs
    for item in items:
        if item["transaction"] == "CHECK_OUT":
            acm_out.append(item["acm_name"])
    for item in items:
        if item["transaction"]=="CHECK_IN" and item["acm_name"] not in acm_out:
            print str(item["acm_name"])
    return



if __name__=="__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('-m', '--mode', help="Select from 'all', 'in', 'out' modes to list all ACMs, checked-out, or "
                                             "checked-in ACMs ", default="all")
    args = parser.parse_args()
    status_check(args.mode)