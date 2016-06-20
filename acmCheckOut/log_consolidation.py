"""
log_consolidation.py

An AWS lambda function that executes at the rate once per day.
Consolidates all of yesterday's ACM checkout log files into a single .log file with yesterday's date as the filename
and deletes the individual logs.

Log file storage location: s3 bucket 'acm-logging'
Daily log: 'logs/YEAR/MONTH/DAY.log'

AWS Limits:
File Size: 5GB (for single put object request)
Max object size in s3: 5TB

NOTE: if you need to aggregate and create a daily log file for an arbitrary day, run function from the command line
            python log_consolidation.py -d --date 'MM-DD-YYYY' (with desired date in proper format)
"""
from __future__ import print_function
import datetime
import boto3
import argparse
import time

# specify s3 client where logs are stored
client = boto3.client('s3', region_name='us-west-2')
bucket = 'acm-logging'

# we want to aggregate yesterday's logs
yesterday = datetime.date.fromordinal(datetime.date.today().toordinal()-1)

def lambda_handler(event, context, date=yesterday):
    """
    :param event: required by AWS - not used
    :param context: required by AWS - not used
    :param date: datetime object -- consolidate logs from gitthis date (default: yesterday)
    :return: none
    """
    # specify s3 subdirectory where logs are stored ( i.e. logs/YEAR/MONTH )
    base_dir = 'logs/'+ str(date.year) + '/' + str(date.month)

    t=time.time()
    # pull from yesterdays subdirectory in the bucket
    response = client.list_objects(
        Bucket=bucket,
        Prefix=base_dir + '/' + str(date)
    )
    print('pulling yesterday:', time.time()-t)

    # early return if there are no logs for specified date
    if not response.get('Contents'):
        return

    t=time.time()
    # grab all of yesterdays logs
    logs = [log.get('Key') for log in response.get('Contents')]
    check_truncation(response, logs, base_dir, date)
    print('grabbing all of yesterdays logs:', time.time() - t)

    t=time.time()
    # consolidate all log files into a single object
    new_log = aggregate_logs(logs)
    print('reading + aggregating files:',time.time()-t)

    t=time.time()
    # upload consolidated log to s3 bucket with key in format: 'logs/YEAR/MONTH/DAY.log'
    upload_log = client.put_object(    # NOTE: AWS size limitations listed at top of file
        Body=new_log,
        Bucket=bucket,
        ContentType='text/plain',
        Key=base_dir + '/' + 'log_'+str(date)
    )
    print('uploading log:',time.time()-t)

    t=time.time()
    # delete yesterday's individual log files
    if is_successful(upload_log):
        delete_logs(logs)
    print('deleting yesterdays logs:',time.time()-t)


# reads content of each log file and joins all into one buffer
def aggregate_logs(logs):
    body = []
    for log in logs:
        file = client.get_object(
            Bucket=bucket,
            Key=log,
        )
        body.append(file['Body'].read())
    return '\n'.join(body)


# deletes a list of objects from the bucket
def delete_logs(logs):
    # format log list for deletion
    object_list = []
    for log in logs:
        obj = {'Key': log}
        object_list.append(obj)
    print(len(object_list))
    # max number of keys that can be specified = 1000
    for i in range(0, len(object_list), 1000):
        l = client.delete_objects(
            Bucket=bucket,
            Delete={
                'Objects': object_list[i:i+1000]
            })
    return


# recursive helper function to ensure all logs are retrieved from bucket
def check_truncation(response, logs, base_dir, date):
    if response.get('IsTruncated'):
        print('is truncated')
        new_resp = client.list_objects(
            Bucket=bucket,
            Marker=logs[-1],
            Prefix=base_dir + '/' + str(date)
        )
        new_logs = [log.get('Key') for log in new_resp.get('Contents')]
        logs.extend(new_logs)
        check_truncation(new_resp, logs, base_dir, date)
    return


# helper function to check if a request was successful
def is_successful(response):
    code = response.get('ResponseMetadata').get('HTTPStatusCode')
    return code == 200


# run function from command line with specified date
if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('-d', '--date', help='Enter date in format DD-MM-YYYY', default=yesterday)
    args = parser.parse_args()
    # convert string to date object
    date = datetime.datetime.strptime(args.date, "%d-%m-%Y").date()
    print('Consolidating logs from date:', date)
    # consolidate logs
    lambda_handler(None, None, date)
    print('Executed function, check s3 bucket:', bucket)