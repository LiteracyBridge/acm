"""
setupIAM.py
A script that creates an AWS IAM user & attaches policies required to manage ACM checkout procedure.
Can be run from terminal with optional arguments:    -u, --username    desired IAM user name
                                                     -p, --policies    list of policies to attach to IAM user
"""
import boto3
import argparse

# required policies to perform all AWS ACM checkout tasks
policies = ['AWSLambdaFullAccess', 'IAMFullAccess', 'AmazonAPIGatewayAdministrator',
                'AmazonAPIGatewayInvokeFullAccess', 'AmazonDynamoDBFullAccess',
                'AWSCloudTrailFullAccess']

def create_IAM(user_name='testACM', policy_list=policies):
    """
    Create a new AWS IAM user & attach specified policies.
    :param user_name: string -- name for new IAM user (default: 'testACM')
    :param policy_list: list -- policies to attach (default: policies)
    :return: None
    """
    try:
        client = boto3.client('iam')    #specify amazon service to be used
        user = client.create_user(
            UserName=user_name
        )
        print 'Successfully created user: ', user['User']
        # attach specified policies to new IAM user
        for policy in policy_list:
            resp = client.attach_user_policy(
                UserName=user_name,
                PolicyArn='arn:aws:iam::aws:policy/'+policy    # specify amazon resource name of policy
                                                               # e.g. 'arn:aws:iam::aws:policy/AWSLambdaFullAccess'
            )
            print 'Attached policy: ', policy
    except Exception as err:
        print 'ERROR: ', err

if __name__=="__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('-u', '--username', help='Desired IAM user name', default = "testACM")
    parser.add_argument('-p', '--policies', help='List of policies to attach to IAM user', default = policies)
    args = parser.parse_args()
    create_IAM(args.username, args.policies)