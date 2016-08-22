"""
status.py

A CLI script that allows a user to query the status of ACMs in the database.

To run, in the command line type: 'python status.py -a ACM_NAME --out --pretty'

optional arguments:  ACM_NAME   retrieve info on only this one ACM
                     --out      retrieve info on only checked-out ACMs
                     --pretty   only retrieves: acm name, state, last in name & date, now out name & date

default: 'python status.py'     retrieve all ACMs from db

"""
import boto3
import argparse
import sys

# specify back-end dynamoDB table
TABLE = 'acm_check_out'
dynamodb = boto3.resource('dynamodb', region_name='us-west-2')
table = dynamodb.Table(TABLE)
PRETTY_KEYS = ['acm_name', 'acm_state', 'now_out_name', 'now_out_date', 'last_in_name', 'last_in_date']
PRETTY_LABELS = {'acm_name':'ACM',
               'acm_state':"Status",
               'last_in_name':'Last In By',
               'last_in_date':"Date",
               'now_out_name':'Now Out By',
               'now_out_date':'Date'
               }

def status(acm_name=None, only_out=False, verbose=False, all=False):
    # if pretty:
    #     print "---------------------------------------------------------------"
    #     print "ACM Status Check"
    #     print "---------------------------------------------------------------"
    acms = []
    if verbose or all:
        # all acms for verbose output
        name_re = re.compile('.*')
    elif not acm_name:
        # 'acm-' followed by anything other than 'test'
        name_re = re.compile('(?i)ACM-(?!TEST).+')
    else:
        # whatever the user specified, ignoring case
        name_re = re.compile('(?i)' + acm_name)

    # no acm specified: query all acm entries from db
    acms = table.scan(TableName=TABLE).get('Items')

    if not acms:    # early exit if nothing found in table
        print('Table is empty')
        sys.exit(2)

    for acm in acms:
        if only_out and acm.get('acm_state') == "CHECKED_IN":
            continue
        else:
            print_record(acm, pretty)


# helper function that formats & prints records
def print_record(acm, pretty):
    if pretty:  # specify what parameters to retrieve
        params = ['acm_name', 'acm_state', 'last_in_name', 'last_in_date', 'now_out_name', 'now_out_date']
        print ','.join([param + ':' + '"' + str(acm.get(param)) + '"' for param in params])
    else:
        print ','.join(str(k) + ':' + '"' + str(v) + '"' for k, v in sorted(acm.items()))



if __name__=="__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('--out', action='store_true', help='Enable to print only checked-out ACMs')
    parser.add_argument('--verbose', '-v', action='store_true', help='Enable to print all ACM data')
    parser.add_argument('--acm', '-a', help="Check status of a particular acm", default=None)
    parser.add_argument('--all', action='store_true', help="Check status of all particular acms", default=None)
    args = parser.parse_args()
    status(args.acm, args.out, args.verbose, args.all)
