from pathlib import Path
import os.path

import boto3

# boto3 documentation
# https://boto3.amazonaws.com/v1/documentation/api/latest/reference/services/iot.html

ROOT_DIR = os.path.abspath(os.curdir)

KEYS_AND_CERTS_DIR = os.path.join(ROOT_DIR + "/resources/script_certs", "keysAndCerts")

# Things created using this script will have this type name
THING_TYPE_NAME = "Script_created"
THING_ARN_PREFIX = "arn:aws:iot:eu-west-1:830603503526:thing/"

POLICY_NAME = 'pub-sub-policy'

THING_NAMES_CSV = ROOT_DIR + "/resources/trip_sim/device_ids/list_devices.txt"


def setup_certs_and_store(thing_name, client):
    try:
        response = client.create_keys_and_certificate(setAsActive=True)
        # response = get_fake_create_certs_response(provide_secret_key=True)
    except Exception as e:
        print(e)
        return

    try:
        store_credentials(response, thing_name)
    except Exception as e:
        print(e)
    return response


def store_credentials(create_cert_response, thing_name=None):
    dir_path = get_dir_for_this_thing(create_cert_response['certificateId'], thing_name)

    # store the cert
    cert_postfix = "-certificate.pem.crt"
    cert_id = create_cert_response['certificateId']
    cert_filename = cert_id + cert_postfix
    cert_file_content = create_cert_response['certificatePem']
    cert_filepath = os.path.join(dir_path, cert_filename)
    with open(cert_filepath, 'w') as file:
        file.write(str(cert_file_content))

    # store the private key
    private_key_postfix = "-private.pem.key"
    private_key_filename = cert_id + private_key_postfix
    private_key_file_content = create_cert_response['keyPair']['PrivateKey']
    private_key_filepath = os.path.join(dir_path, private_key_filename)
    with open(private_key_filepath, 'w') as file:
        file.write(str(private_key_file_content))

    # store the public key
    public_key_postfix = "-public.pem.key"
    public_key_filename = cert_id + public_key_postfix
    public_key_file_content = create_cert_response['keyPair']['PublicKey']
    public_key_filepath = os.path.join(dir_path, public_key_filename)
    with open(public_key_filepath, 'w') as file:
        file.write(str(public_key_file_content))

    return create_cert_response


def get_dir_for_this_thing(cert_id, thing_name=None):
    if thing_name:
        subdir_name = thing_name
    else:
        subdir_name = cert_id + "_credentials"
    dir_path = os.path.join(KEYS_AND_CERTS_DIR, subdir_name)
    if not os.path.exists(dir_path):
        os.makedirs(dir_path)
    return dir_path


def create_one_thing_on_iot(thing_name, client):
    thing_type_name = THING_TYPE_NAME
    try:
        response = client.create_thing(thingName=thing_name, thingTypeName=thing_type_name)
    except Exception as e:
        print(e)
        raise e
    return response


def create_one_thing_on_iot_with_cert_id(cert_id, client):
    thing_name = get_default_thing_name(cert_id)
    return create_one_thing_on_iot(thing_name, client)


def get_thing_names_from_csv(filename):
    with open(filename, 'r') as file:
        lines = file.readlines()
        list = []
        for line in lines:
            list.append(line.strip())
    return list


def get_default_thing_name(cert_id):
    thing_name = "script_created_thing_" + cert_id[:8]
    return thing_name


def attach_one_cert_to_thing(thing_name, cert_arn, client):
    try:
        response = client.attach_thing_principal(thingName=thing_name, principal=cert_arn)
    except Exception as e:
        print(e)
        response = {}
    return response


def attach_one_policy_to_one_cert(policy_name, cert_arn, client):
    try:
        response = client.attach_policy(policyName=policy_name, target=cert_arn)
    except Exception as e:
        print(e)
        response = {}

    return response


def main():
    client = boto3.client('iot')
    thing_names = get_thing_names_from_csv(THING_NAMES_CSV)
    for thing_name in thing_names:
        print("Created certificates for " + thing_name)
        r1 = setup_certs_and_store(thing_name, client)
        r2 = create_one_thing_on_iot(thing_name, client)
        _ = attach_one_cert_to_thing(r2['thingName'], r1['certificateArn'], client)
        _ = attach_one_policy_to_one_cert(POLICY_NAME, r1['certificateArn'], client)

if __name__ == '__main__':
    main()
