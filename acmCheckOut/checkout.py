'''
checkout.py

An AWS lambda function that handles ACM checkout requests.
Processes POST requests sent to API Gateway ('ACMCheckOut' API) from the java ACM checkout program.
Checks ACMs in or out, updates backend DynamoDB table ('acm_check_out'), and returns a stringified JSON object
to client based on success of requested transaction.

Six available transactions:
'checkOut
'checkIn
'statusCheck'
'revokeCheckOut'
'discard'
'new' (subroutine of checkIn)

NOTE: To be uploaded and saved to AWS Console as lambda function 'acmCheckOut' for proper integration with API Gateway

DynamoDB table schema:
acm_check_out = {
    acm_name: { type: String, primaryKey: true, required: true },
    acm_state: { type: String, required: false },
    acm_comment: { type: String, required: false },
    last_in_name: { type: String, required: false },
    last_in_contact: { type: String, required: false },
    last_in_date: { type: String, required: false },
    last_in_comment: { type: String, required: false },
    last_in_file_name: { type: String, required: false },
    now_out_name: { type: String, required: false },
    now_out_contact: { type: String, required: false },
    now_out_date: { type: String, required: false },
    now_out_version: { type: String, required: false },
    now_out_comment: { type: String, required: false },
    now_out_key: { type: String, required: false }
}

'''
from __future__ import print_function
from random import randint
import boto3
import datetime
from boto3.dynamodb.conditions import Key, Attr

REGION_NAME = 'us-west-2'
BUCKET_NAME = 'acm-logging'
TABLE_NAME = 'acm_check_out'
CHECKED_IN = 'CHECKED_IN'
CHECKED_OUT = 'CHECKED_OUT'
START_TIME = str(datetime.datetime.now())

# specify s3 bucket to store logs
s3 = boto3.resource('s3', region_name=REGION_NAME)
bucket = s3.Bucket(BUCKET_NAME)

# specify dynamoDB table
dynamodb = boto3.resource('dynamodb', region_name=REGION_NAME)
table = dynamodb.Table(TABLE_NAME)


def lambda_handler(event, context):
    '''
    :param event: dict -- POST request passed in through API Gateway
    :param context: object -- can be used to get runtime data (unused but required by AWS lambda)
    :return: a JSON string object that contains the status of transaction & information required by java program
    '''
    acm = None
    exp_date = None

    try:
        # parameters received from HTTPS POST request:
        acm_name = event.get('db')    # name of ACM (e.g. 'ACM-FB-2013-01') - primary key of dynamoDB table
        action = event.get('action')  # 'checkIn', 'revokeCheckOut', 'checkOut', 'statusCheck', 'discard'
        check_key = event.get('key')  # distinguish between ACM creation (i.e. key = 'new') or standard check-in



        # query table to get stored acm entry from db
        query_acm = table.get_item(Key={'acm_name': acm_name})
        acm = query_acm.get('Item')

        # available transactions
        if action == 'revokeCheckOut':
            return revoke_check_out(event)
        if action == 'discard':
            return discard(event)
        if action == 'statusCheck':
            return status_check(acm)
        if action == 'checkIn':
            if check_key == 'new':
                return new_check_in(event, acm)
            return check_in(event, acm)
        if action == 'checkOut':
            return check_out(event, acm)
        # user requested an unknown action
        logger(event, 'denied')
        return {
            'data': 'denied',
            'response': 'Unknown action requested'
        }
    except Exception as err:
        logger(event, 'denied')
        return {
            'response': 'Unexpected Error',
            'data': 'denied',
            'error': str(err)
        }


def check_in(event, acm):
    '''
    A successful 'checkIn' request deletes the existing ACM check-out parameters from the db and updates the check-in
    entry with the requested user's info iff specified conditions are met (to protect against multi-user overwrites).

    :param event: dict -- data passed in through POST request
    :param exp_date: string -- used for conditional update of check-in entry
    :param acm: dict -- stored acm info from db
    :param table: object -- dynamoDB table
    :return: a stringified JSON object (similar to check_out)

            must return following data parameters for each event case:
                SUCCESS -- 'ok'
                FAILURE -- 'denied'
    '''
    # early return if ACM does not exist
    if not acm:
        logger(event, 'denied')
        return {
            'response': 'Create new ACM',
            'data': 'denied'
        }

    # early return if ACM not checked-out
    if acm.get('acm_state') == 'CHECKED_IN':
        logger(event, 'denied')
        return {
            'response': 'ACM is already checked-in',
            'data': 'denied'
        }

    # parameters received from HTTPS POST request
    check_key = event.get('key')  # key must match check-key assigned at ACM check-out for check-in
    file_name = event.get('filename')  # tracks number of times ACM has been checked out, format db##.zip (e.g. db12.zip)
    user_name = event.get('name')  # name of requester
    contact = event.get('contact')  # phone number of requester

    # passed into AWS update table item function: delete check-out info from entry and update check-in info
    update_exp = 'REMOVE now_out_name, now_out_contact, now_out_date, now_out_key, now_out_comment \
                SET acm_state = :s, last_in_file_name = :f, last_in_name = :n, last_in_contact = :c, last_in_date = :d, \
                last_in_comment = now_out_comment'
    condition_exp = 'now_out_key = :k and now_out_name = :n'  # only perform check-in if THIS user has THIS check-out
    exp_values = {    # expression values (required by AWS boto3)
        ':k': check_key,
        ':s': CHECKED_IN,
        ':f': file_name,
        ':n': user_name,
        ':c': contact,
        ':d': START_TIME
    }

    # JSON response parameters
    json_resp = {
        'success': {'response': 'SUCCESS. Checked in by ' + str(user_name), 'data': 'ok'},
        'failure': {'response': 'FAILED. Do not have permission to perform check-in', 'data': 'denied'}
    }

    # update dynamodb
    return update_dynamo(event, update_exp, condition_exp, exp_values, json_resp)


def new_check_in(event, acm):
    '''
    Allows the user to create a new ACM by specifying the action as 'checkIn' and the key as 'new.' Only works for ACMs
    that do not already exist

    :param all parameters same as check_in function
    :return: same as check_in function -- a stringified JSON object

            must return following data parameters for each event case:
                SUCCESS -- 'ok'
                FAILURE -- 'denied'
    '''
    if acm:
        logger(event, 'denied')
        return {
            'response': 'ACM already exists',
            'data': 'denied'
        }

    # parameters received from HTTPS POST request
    file_name = event.get('filename')  # tracks number of times ACM has been checked out, format db##.zip (e.g. db12.zip)
    user_name = event.get('name')  # name of requester
    contact = event.get('contact')  # phone number of requester
    comment = event.get('comment')  # to allow for internal comments if later required

    # passed to AWS update table item function: create entry for ACM in db
    update_exp = 'SET acm_state = :s, last_in_file_name = :f, last_in_name = :n, last_in_contact = :c, last_in_date = :d, \
                acm_comment = :v'
    condition_exp = 'attribute_not_exists(last_in_name)'  # only perform if no check-in entry exists
    exp_values = {
        ':s': CHECKED_IN,
        ':f': file_name,
        ':n': user_name,
        ':c': contact,
        ':d': START_TIME,
        ':v': comment
    }

    # JSON response parameters
    json_resp = {
        'success': {'response': 'SUCCESS. Created new ACM', 'data': 'ok'},
        'failure': {'response': 'FAILED. Do not have permission to perform check-in', 'data': 'denied'}
    }

    return update_dynamo(event, update_exp, condition_exp, exp_values, json_resp)


def discard(event):
    '''
    Allows user to delete his/her own ACM check-out entry from the db (i.e. if the check-out key and user-names match)

    :param event: dict -- data passed in through POST request
    :param table: object -- dynamoDB table
    :return: a stringified JSON object

            must return following data parameters for each event case:
                SUCCESS -- 'ok'
                FAILURE -- 'denied'
    '''
    # passed in POST parameters
    acm_name = event.get('db')
    user_name = event.get('name')
    check_key = event.get('key')

    # passed to AWS update table item function: delete check-out info and mark ACM as checked-in
    update_exp = 'REMOVE now_out_name, now_out_contact, now_out_date, now_out_key, now_out_comment SET acm_state = :s'
    condition_exp = 'now_out_key = :k and now_out_name = :n'  # only perform check-in if THIS user has THIS check-out
    exp_values = {
        ':s': CHECKED_IN,
        ':k': check_key,
        ':n': user_name
    }

    # JSON response parameters
    json_resp = {
        'success': {'response': 'SUCCESS. Discarded check-out info', 'data': 'ok'},
        'failure': {'response': 'FAILED. Do not have permission to perform discard', 'data': 'denied'}
    }

    return update_dynamo(event, update_exp, condition_exp, exp_values, json_resp)


def check_out(event, acm):
    '''
    A successful 'checkOut' request creates the ACM check-out entry in the db with the requested user's info iff a
    check-out entry doesn't already exist (conditional update to protect against multi-user overwrites)

    :param event: dict -- data passed in through POST request
    :param acm: dict -- stored acm info from db
    :param table: object -- dynamoDB table

    :return: a stringified JSON object that reflects the status of transaction containing a user-readable response,
            data that will be parsed by the java ACM checkout program, & any errors

            must return following data parameters for each event case:
                SUCCESS checked out         -- 'key=RANDOM_KEY', 'filename=COUNTER '
                FAILURE already checked out -- 'possessor=NAME', 'filename=COUNTER '
                FAILURE ACM does not exist  -- 'filename=NULL'
                FAILURE error occurred      -- 'denied'    ###THIS NEEDS TO BE IMPLEMENTED ON JAVA SIDE
    '''
    # parameters received from HTTPS POST request
    user_name = event.get('name')  # name of requester
    contact = event.get('contact')  # phone number of requester
    version = event.get('version')  # current ACM version in format: r YY MM DD n (e.g. r1606221)
    comment = event.get('comment')  # to allow for internal comments if later required

    # Make sure ACM is available to be checked out
    status = status_check(acm)
    if status['response'] != 'ACM available':
        return status

    # Create a check-out entry for ACM in table
    new_key = str(randint(0, 10000000))    # generate new check-out key to use when checking ACM back in

    update_exp = 'SET acm_state = :s, now_out_name = :n, now_out_contact = :c, now_out_key = :k, \
                            now_out_version = :v, now_out_comment = :t, now_out_date = :d'
    condition_exp = 'attribute_not_exists(now_out_name)'    # check for unexpected check-out entry
    exp_values = {
        ':s': CHECKED_OUT,
        ':n': user_name,
        ':c': contact,
        ':k': new_key,
        ':v': version,
        ':t': comment,
        ':d': START_TIME
    }

    # JSON response parameters
    json_resp = {
        'success': {'response': 'SUCCESS. Checked out to ' + str(user_name),
                    'data': {'1': 'key=' + new_key,
                             '2': 'filename=' + str(acm.get('last_in_file_name'))}
                    },
        'failure': {'response': 'Your transaction was intercepted.', 'data': 'denied'}
        # data: to be updated with interceptor's name & new file_name in update_dynamo function
    }

    return update_dynamo(event, update_exp, condition_exp, exp_values, json_resp)


def revoke_check_out(event):
    '''
    A successful 'revokeCheckOut' request deletes any ACM check-out entry from the db.
    :param event: dict -- data passed in through POST request
    :return: a stringified JSON object

            must return following data parameters for each event case:
                SUCCESS -- 'ok'
                FAILURE -- 'denied'
    '''

    update_exp = 'SET acm_state = :s REMOVE now_out_name, now_out_contact, now_out_date, now_out_key, \
                            now_out_comment'
    condition_exp = ':s = :s'  # revoke check-out should always execute
    exp_values = {
        ':s': CHECKED_IN,
    }

    # JSON response parameters
    json_resp = {
        'success': {'response': 'Deleted check out entry', 'data': 'ok'},
        'failure': {'response': 'Unexpected Error', 'data': 'denied'}
        # data: updated with interceptor's name & new file_name in update_dynamo function
    }

    return update_dynamo(event, update_exp, condition_exp, exp_values, json_resp)


def status_check(acm):
    '''
    Queries check-out status of ACM.

    :param acm: dict -- row entry for acm in db
    :return: stringified JSON object of ACM status

            must return following data parameters for each event case:
                FAILURE already checked out -- 'possessor=NAME filename=COUNTER'
                FAILURE ACM does not exist  -- 'filename=NULL'
                FAILURE error occurred      -- 'denied'    ###THIS NEEDS TO BE IMPLEMENTED ON JAVA SIDE
    '''
    # First time for a new ACM
    if not acm:
        return {
            'response': 'Create new ACM',
            'data': 'filename=NULL'    # filename:tracks number of times acm has been checked-out
        }
    if acm.get('acm_state') == 'CHECKED_OUT':
        return {
            'response': 'Already checked out',
            'data': {'1': 'possessor=' + str(acm.get('now_out_name')),
                     '2': 'filename=' + str(acm.get('last_in_file_name'))}
        }
    return {
        'response': 'ACM available',
        'data': 'filename='+str(acm.get('last_in_file_name'))
    }


# helper function to update dynamoDB
def update_dynamo(event, update_exp, condition_exp, exp_values, json_resp):
    '''
    :param event: dict -- data passed in through POST request
    :param update_exp: string -- dynamodb table update expression
    :param condition_exp: string -- conditional update expression
    :param exp_values: string -- expression values (required by AWS)
    :param json_resp: dict -- json response data
    :return: JSON response
    '''
    # parameters received from HTTPS POST request
    acm_name = event.get('db')  # name of ACM (e.g. 'ACM-FB-2013-01') - primary key of dynamoDB table
    action = event.get('action')

    try:
        table.update_item(
            Key={
                'acm_name': acm_name
            },
            UpdateExpression= update_exp,
            ConditionExpression= condition_exp,
            ExpressionAttributeValues= exp_values
        )
        logger(event, 'ok')
        return{
            'response': json_resp['success']['response'],
            'data': json_resp['success']['data']
        }
    except Exception as err:
        # transaction intercepted: unexpected error or conditional check failed
        if 'ConditionalCheckFailedException' in str(err):
            query_acm = table.get_item(    # retrieve acm entry from db
                Key={
                    'acm_name': acm_name,
                })
            acm = query_acm.get('Item')
            if acm:
                if not acm.get('now_out_name'):   # no check-out info exists
                    if action == 'discard':    # interceptor deleted check-out for us - consider it success!
                        logger(event, 'nop')    # log as no operation
                        return {
                            'response': json_resp['success']['response'],
                            'data': json_resp['success']['data']
                        }
                logger(event, 'denied')
                if action == 'checkOut':    # update with interceptor name & new filename
                    json_resp['failure']['data'] = {'1': 'possessor='+str(acm.get('now_out_name')),
                                                    '2': 'filename='+str(acm.get('last_in_file_name'))}
                return {
                    'response': json_resp['failure']['response'],
                    'data': json_resp['failure']['data']
                }
        logger(event, 'denied')
        return {
            'response': 'Unexpected Error',
            'data': 'denied',
            'error': str(err)
        }


# helper function to write plain txt log files & upload to s3 bucket 'acm-logging'
def logger(event, response):
    '''
    Writes a single line .txt file with filename as current datetime + random integer in s3 bucket 'acm-logging'
    in the 'logs/year/month' directory.

    Each log string contains the following parameters:
        datetime, action, response, acm_name, name, contact, computer_name, version, file_name, check_key

    :param event: dict -- passed in parameters from POST request
    :param response: string -- transaction result
    :return: None -- creates an s3 object
    '''
    acm_name = str(event.get('db'))    # name of ACM (e.g. 'ACM-FB-2013-01') - primary key of dynamoDB table
    action = str(event.get('action'))  # 'checkIn', 'revokeCheckOut', 'checkOut', 'statusCheck', 'discard'
    check_key = str(event.get('key'))  # key must match check-key assigned at ACM check-out for check-in
    file_name = str(event.get('filename'))  # tracks no. of times ACM has been checked out, format db#.zip (e.g. db8.zip)
    name = str(event.get('name'))  # name of requester
    contact = str(event.get('contact'))  # phone number of requester
    version = str(event.get('version'))  # current ACM version in format: r YY MM DD n (e.g. r1606221)
    computer_name = str(event.get('computername'))  # IP address of request
    comment = str(event.get('comment'))  # to allow for internal comments if later required
    date = datetime.date.today()

    log = [START_TIME, action, response, acm_name, name, contact, computer_name, version, comment]
    if action == 'checkIn':
        log = [START_TIME, action, response, acm_name, name, contact, computer_name, version, file_name, check_key, comment]

    try:
        response = bucket.put_object(
            Body=','.join('"' + item + '"' for item in log),    # quotified log string
            Key='logs/'+ str(date.year) + '/' + str(date.month) + '/' + START_TIME + '_' + str(randint(0,1000)), # unique filename
            ContentType='text/plain',
        )
    except Exception as err:
        print ('LOGGING ERROR: ' + str(err))
        pass