#!/usr/local/bin/python
"""
status.py

A CLI script that allows a user to query the status of ACMs in the database.

To run, in the command line type: 'python status.py -a ACM_NAME --out --pretty'

optional arguments:  ACM_NAME   retrieve info on only this one ACM
                     --out      retrieve info on only checked-out ACMs
                     --pretty   only retrieves: acm name, state, last in name & date, now out name & date

default: 'python status.py'     retrieve all ACMs from db

"""

import argparse
import re
import sys

import boto3

# specify back-end dynamoDB table
TABLE = 'acm_check_out'
dynamodb = boto3.resource('dynamodb', region_name='us-west-2')
table = dynamodb.Table(TABLE)
PRETTY_KEYS = ['acm_name', 'acm_state', 'now_out_name', 'now_out_date', 'last_in_name', 'last_in_date', 'last_in_file_name']
PRETTY_LABELS = {'acm_name':'ACM',
               'acm_state':"Status",
               'last_in_name':'Last In By',
               'last_in_date':"Checkin Date",
               'last_in_file_name':"In File",
               'now_out_name':'Now Out By',
               'now_out_date':'Checkout Date'
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

    # Get list of acms
    acms = table.scan(TableName=TABLE).get('Items')
    if len(acms) == 0:
        sys.stderr.write('No ACM(s) found\n')
        exit(3)

    # Match by name
    acms = [acm for acm in acms if name_re.search(acm.get('acm_name'))]
    if len(acms) == 0:
        sys.stderr.write('No matching ACM(s)\n')
        exit(2)

    # Status, if desired
    if only_out:
        acms = [acm for acm in acms if acm.get('acm_state') == "CHECKED_OUT"]
    if len(acms) == 0:
        sys.stderr.write('No matching checked-out ACM(s)\n')
        exit(1)

    # Sort to be easier to find an ACM (not needed now, but maybe someday)
    acms = sorted(acms, key=lambda x: x.get('acm_name'))

    print_records(acms, not verbose)

# Format and print for all records
def print_records(acms, pretty):
    if pretty:
        widths = get_widths(acms)
        print_headings(widths)
        for acm in acms:
            print_pretty(acm, widths)
    else:
        for acm in acms:
            print_simple(acm)

# Simple format, all fields
def print_simple(acm):
    print ','.join(str(k) + ':' + '"' + str(v) + '"' for k, v in sorted(acm.items()))

# Get widths for pretty-printing
def get_widths(acms):
    widths = {}
    # At least as wide as the key name
    for k in PRETTY_KEYS:
        widths[k] = len(PRETTY_LABELS[k])
    # And as wide as the widest value
    for acm in acms:
        for k in PRETTY_KEYS:
            widths[k] = max(widths.get(k,0), len(acm.get(k, '')))
    return widths

# Print the headings for a pretty listing
def print_headings(widths):
    lbl = ''
    eql = ''
    for k in PRETTY_KEYS:
        w = widths[k]
        lbl += '{0:{width}} '.format(PRETTY_LABELS.get(k, ''), width=w)
        eql += '{0:=<{width}} '.format('', width=w)
    print(lbl)
    print(eql)

# Print a pretty line
def print_pretty(acm, widths):
    str = ''
    for k in PRETTY_KEYS:
        w = widths[k]
        str += '{0:{width}} '.format(acm.get(k, ''), width=w)
    print(str)

if __name__=="__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('--out', action='store_true', help='Enable to print only checked-out ACMs')
    parser.add_argument('--verbose', '-v', action='store_true', help='Enable to print all ACM data')
    parser.add_argument('--acm', '-a', help="Check status of a particular acm", default=None)
    parser.add_argument('--all', action='store_true', help="Check status of all particular acms", default=None)
    args = parser.parse_args()
    status(args.acm, args.out, args.verbose, args.all)
