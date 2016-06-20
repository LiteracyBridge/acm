"""
setupAPI.py
A script that creates an API using AWS API Gateway to handle ACM checkout requests. To be run once or
duplicate APIs will be created.
Can be run from terminal with optional arguments:    -a, --apiname    desired API gateway name
                                                     -m, --method     HTTP request type (i.e. POST or GET)
                                                     -u, --uri        ARN link to backend AWS service
"""
import boto3
import argparse

# default arn from api gateway to acmCheckOut lambda function
lambda_arn='arn:aws:apigateway:us-west-2:lambda:path/2015-03-31/functions/' \
           'arn:aws:lambda:us-west-2:856701711513:function:acmCheckOut/invocations'

def create_API(api_name='test_api', method='POST', uri=lambda_arn):
    """
    Create a new API that handles ACM checkout POST requests & integrates with backend lambda checkout function.
    :param name: string -- name for new API (default = 'test_api')
    :param method: string -- HTTP Request received from client (default = 'POST')
    :param uri: string -- amazon resource name:: links api gateway to lambda function (default link to "acmCheckOut")
    :return: None
    """
    try:
        client = boto3.client('apigateway')    # specify amazon service to be used
        api = client.create_rest_api(
            name=api_name,
        )
        print 'Created API: ', api
        api_id = api['id']
        # get root ("/") resource id in order to assign POST method request & response
        resource = client.get_resources(
            restApiId=api_id,
        )
        resource_id = resource['items'][0]['id']    # parse resource (dict) for list of resources (dicts) to get id
        # create a POST method under root resource
        post = client.put_method(
            restApiId=api_id,
            resourceId=resource_id,
            httpMethod=method,
            authorizationType='NONE'    # can specify custom authorization or AWS_IAM auth if desired
        )
        print 'Added POST method'
        # create an integration request to link method to backend AWS lambda function
        link = client.put_integration(
            restApiId=api_id,
            resourceId=resource_id,
            httpMethod=method,
            type='AWS',    # specify integration backend (i.e. 'MOCK' | 'HTTP' | 'AWS')
            integrationHttpMethod=method,
            uri=uri    # format: 'arn:aws:apigateway:region:lambda:path/2015-03-31/functions/functionArn/invocations'
        )
        print 'Linked to backend lambda function'
        # create a method response based on default api gateway scheme - only need to specify for status-code 200
        resp = client.put_method_response(
            restApiId=api_id,
            resourceId=resource_id,
            httpMethod=method,
            statusCode='200',
            responseModels={
                "application/json": "Empty"    # required default response model
            }
        )
        print 'Added POST response'
        # create an integration response to link lambda checkout response to method response
        linkResp = client.put_integration_response(
            restApiId=api_id,
            resourceId=resource_id,
            httpMethod=method,
            statusCode='200',
            responseTemplates={
                "application/json": "Empty"    # required default response template
            }
        )
        print 'Linked to integration response'
    except Exception as err:
        print 'ERROR: ', err

if __name__=="__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('-a', '--apiname', help='Desired API gateway name', default = "test_api")
    parser.add_argument('-m', '--method', help='HTTP method type (POST or GET)', default = "POST")
    parser.add_argument('-u', '--uri', help='ARN that links API to backend lambda function', default = lambda_arn)
    args = parser.parse_args()
    create_API(args.apiname, args.method, args.uri)