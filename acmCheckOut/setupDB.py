"""
setupDB.py
A script that sets up an AWS dynamoDB table for the ACM checkout procedure.
Can be run from terminal with optional argument:    -t, --tablename    desired dynamoDB table name
"""
import boto3
import argparse


def create_DB(table_name='test_acm_check_out'):
    """
    Create a dynamoDB table to store ACM checkout transactions with the following parameters:
            Primary partition key: acmId (string)
            Primary sort key: transaction (string)
            Provisioned read capacity units: 1
            Provisioned write capacity units: 1
    :param table_name: string -- name for new table (default = 'test_acm_check_out')
    :return: None
    """
    try:
        client = boto3.client('dynamodb')    # specify amazon service to be used
        table = client.create_table(
            AttributeDefinitions=[    # specify attribute names & types required for table's key schema
                {
                    'AttributeName': 'acm_name',    # primary partition key
                    'AttributeType': 'S'    # S = string
                },
                {
                    'AttributeName': 'transaction',    # primary sort key
                    'AttributeType': 'S'
                },
            ],
            TableName=table_name,
            KeySchema=[    # specify primary partition and primary sort keys
                {
                    'AttributeName': 'acm_name',
                    'KeyType': 'HASH'  # HASH = partition key
                },
                {
                    'AttributeName': 'transaction',
                    'KeyType': 'RANGE'  # RANGE = sort key
                },
            ],
            ProvisionedThroughput={    # limit read/write capacity units of table (affects cost of table)
                'ReadCapacityUnits': 1,
                'WriteCapacityUnits': 1
            }
        )
        print 'Successfully created table: ', table['TableDescription']
    except Exception as err:
        print 'ERROR: ', err

if __name__=="__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('-t', '--tablename', help='Desired dynamoDB table name', default = "test_acm_check_out")
    args = parser.parse_args()
    create_DB(args.tablename)
