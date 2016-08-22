"""
log_consolidation.py

An AWS lambda function that executes at the rate once per day.
Consolidates all of yesterday's ACM checkout log files into a single .log file with yesterday's date as the filename
and deletes the individual logs.

Log file storage location: s3 bucket 'acm-logging'
Daily log: 'logs/YEAR/MONTH/DAY.log'

AWS Limits:
File Size: 5GB (for single put object request) -- recommended limit: 300MB
Max object size in s3: 5TB

NOTE: if you need to aggregate and create a daily log file for an arbitrary day, run function from the command line
            python log_consolidation.py -d --date 'YYYY-MM-DD' (with desired date in proper format)
"""
from __future__ import print_function
from __future__ import division
import datetime
import boto3
import argparse
import math
import sys

# specify s3 client where logs are stored
client = boto3.client('s3', region_name='us-west-2')
bucket = 'acm-logging'

# we want to aggregate yesterday's logs
yesterday = datetime.date.fromordinal(datetime.date.today().toordinal()-1)

def lambda_handler(event, context, date=yesterday):
    """
    :param event: required by AWS - not used
    :param context: required by AWS - not used
    :param date: datetime object -- consolidate logs from this date (default: yesterday)
    :return: none
    """
    # specify s3 subdirectory where logs are stored ( i.e. logs/YEAR/MONTH )
    base_dir = 'logs/'+ str(date.year) + '/' + str(date.month)

    # pull from yesterdays subdirectory in the bucket
    response = client.list_objects(
        Bucket=bucket,
        Prefix=base_dir + '/' + str(date)
    )

    # early return if there are no logs for specified date
    if not response.get('Contents'):
        return

    # grab all of yesterday's logs
    logs = [log.get('Key') for log in response.get('Contents')]
    check_truncation(response, logs, base_dir, date)

    # consolidate all log files into a single object
    new_log = aggregate_logs(logs)

    # upload consolidated log to s3 bucket with key in format: 'logs/YEAR/MONTH/log_DAY'
    uploaded = upload(new_log, base_dir, date)

    # delete yesterday's individual logs
    if uploaded:
        delete_logs(logs)


# standard upload by default or use multipart upload if file size is larger than 200MB
def upload(new_log, base_dir, date):
    try:
        # determine if we need to split file up into chunks & do multipart upload
        loglength = len(new_log)
        logsize= int(sys.getsizeof(new_log))
        chunkcount = int(math.ceil(logsize / 209715200))    # Use a chunk size of ~200 MB
        chunklength = int(math.ceil(loglength/chunkcount))    # okay if log chunk is a bit over 200MB

        if chunkcount == 1:
            client.put_object(  # NOTE: AWS size limitations listed at top of file
                Body=new_log,
                Bucket=bucket,
                ContentType='text/plain',
                Key=base_dir + '/' + 'log_' + str(date)
            )
            return True

        # Initiate the multipart upload request
        mp = client.create_multipart_upload(
            Bucket=bucket,
            ContentType='text/plain',
            Key=base_dir + '/' + 'log_' + str(date)
        )
        etags = []

        # Split the log string into chunks
        for i in range(chunkcount):
            print(i)
            offset = chunklength * i
            chars = min(chunklength, loglength - offset)

            part = client.upload_part(
                Body=new_log[offset:offset+chars],
                Bucket=bucket,
                Key=mp.get('Key'),
                PartNumber=(i+1),
                UploadId=mp.get('UploadId')
            )
            etags.append({'PartNumber':i+1, 'ETag':part.get('ETag')})

        # Complete the multipart upload
        client.complete_multipart_upload(
            Bucket=bucket,
            Key=base_dir + '/' + 'log_' + str(date),
            MultipartUpload={'Parts':etags},
            UploadId=mp.get('UploadId')
        )
        uploaded = True

    except Exception as err:
        print("Could not upload logs--retry using command line function: "+ str(err))
        uploaded = False

    return uploaded


# reads content of each log file and joins all into one buffer
def aggregate_logs(logs):
    body = []
    for log in logs:
        file = client.get_object(
            Bucket=bucket,
            Key=log,
        )
        body.append(file['Body'].read())
    return '\n'.join(body)+'\n'


# deletes a list of objects from the bucket
def delete_logs(logs):
    # format log list for deletion
    object_list = []
    for log in logs:
        obj = {'Key': log}
        object_list.append(obj)
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
    parser.add_argument('-d', '--date', help='Enter date in format YYYY-MM-DD', default=str(yesterday))
    args = parser.parse_args()
    # convert string to date object
    date = datetime.datetime.strptime(args.date, "%Y-%m-%d").date()
    print('Consolidating logs from date:', date)
    # consolidate logs
    lambda_handler(None, None, date)
    print('Executed function, check s3 bucket:', bucket)