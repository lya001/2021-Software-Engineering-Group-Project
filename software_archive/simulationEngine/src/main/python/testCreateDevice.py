import os
import unittest
from pathlib import Path
from unittest.mock import MagicMock, Mock

import createDevice

KEYS_AND_CERTS_DIR = createDevice.KEYS_AND_CERTS_DIR

THING_TYPE_NAME = createDevice.THING_TYPE_NAME
THING_ARN_PREFIX = createDevice.THING_ARN_PREFIX

POLICY_NAME = createDevice.POLICY_NAME


def get_fake_create_certs_response():
    response = {
        'certificateArn': 'arn:aws:iot:eu-west-1:830603503526:cert/'
                          '169892a03140255e453f1c8b702af748b1676537ce54fe5ded0e8bfd8c5186c7',
        'certificateId': '169892a03140255e453f1c8b702af748b1676537ce54fe5ded0e8bfd8c5186c7',
        'certificatePem': '-----BEGIN CERTIFICATE-----\n\
MIIDWTCCAkGgAwIBAgIUeO1f+fKair1jaFNdDQM9x+bvUe4wDQYJKoZIhvcNAQEL\n\
BQAwTTFLMEkGA1UECwxCQW1hem9uIFdlYiBTZXJ2aWNlcyBPPUFtYXpvbi5jb20g\n\
SW5jLiBMPVNlYXR0bGUgU1Q9V2FzaGluZ3RvbiBDPVVTMB4XDTIxMTEwODExNDcx\n\
MFoXDTQ5MTIzMTIzNTk1OVowHjEcMBoGA1UEAwwTQVdTIElvVCBDZXJ0aWZpY2F0\n\
ZTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAMapGO6KX4yA1/rpbrjO\n\
bWT1zN+zhw/1NwzdR8+DbDwHzvQMx4tVTckgO89KavUjVeFsegtx6V37ADIPsJoG\n\
QFeg+JvX/WG1ayEx7VFSi6dbOJMP6TClRZ0MA/Yt3MnQ1Zgf/xsla+2heQ/JTitI\n\
BQQs+z9ns7rZzt4qt0OHRbh+Sf767sHV8NNmJZSS50x7sKcyAF5d6kyX6VfY32RW\n\
v2sRqDAfCOdoswR+EhH9L+qo6fe0Rjazzyoz+waJKY8CARQkAsJEIuNCUgIKic9S\n\
Zz6V/rVhKe+BPfv8GpsofyoH7J4txrfSuxGUAbJRIvZexyHB/Q+lgqEqyOc+oiJm\n\
d1cCAwEAAaNgMF4wHwYDVR0jBBgwFoAUF5PR1yYenv48yP5N57mN3ptfEg0wHQYD\n\
VR0OBBYEFNCmImyAKwlSIA084cCOVi+lraWIMAwGA1UdEwEB/wQCMAAwDgYDVR0P\n\
AQH/BAQDAgeAMA0GCSqGSIb3DQEBCwUAA4IBAQBgP3MaUjWquVZEMrNrjtd+8OXb\n\
wcz+vZH+8nSc0W/eFAOYiJsqs/+Qi8yos7zmxfN0AhscMjMbFxJnByZkGGQ+p3jb\n\
8dy78tkEJ4KB97ydqUHTBAsOKR8DEzm3lPP8MoZqYPSp7g811c9cKA2qni31jLWg\n\
pePzSTmhzkazjzUs2VclROPF16Q3PQIszDiXYp2a3tTvMsLFef6c/xn6NlZcHQhu\n\
frfpnfybho7Sr3YAKSHhNS+/PobUBLL9aL//vSZAC88w7WZPBEjxf7Ohvh+W1Ns/\n\
qbVo6X0kca2OF1ktceY0uDcSoP+SGNrmv8U92MiyjWLWrmD4KvrWx9pPHEkC\n\
-----END CERTIFICATE-----',
        'keyPair': {
            'PublicKey': '-----BEGIN PUBLIC KEY-----\n\
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxqkY7opfjIDX+uluuM5t\n\
ZPXM37OHD/U3DN1Hz4NsPAfO9AzHi1VNySA7z0pq9SNV4Wx6C3HpXfsAMg+wmgZA\n\
V6D4m9f9YbVrITHtUVKLp1s4kw/pMKVFnQwD9i3cydDVmB//GyVr7aF5D8lOK0gF\n\
BCz7P2ezutnO3iq3Q4dFuH5J/vruwdXw02YllJLnTHuwpzIAXl3qTJfpV9jfZFa/\n\
axGoMB8I52izBH4SEf0v6qjp97RGNrPPKjP7BokpjwIBFCQCwkQi40JSAgqJz1Jn\n\
PpX+tWEp74E9+/wamyh/Kgfsni3Gt9K7EZQBslEi9l7HIcH9D6WCoSrI5z6iImZ3\n\
VwIDAQAB\n\
-----END PUBLIC KEY-----',
            'PrivateKey': '-----BEGIN RSA PRIVATE KEY-----\n\
MIIEowIBAAKCAQEAxqkY7opfjIDX+uluuM5tZPXM37OHD/U3DN1Hz4NsPAfO9AzH\n\
i1VNySA7z0pq9SNV4Wx6C3HpXfsAMg+wmgZAV6D4m9f9YbVrITHtUVKLp1s4kw/p\n\
MKVFnQwD9i3cydDVmB//GyVr7aF5D8lOK0gFBCz7P2ezutnO3iq3Q4dFuH5J/vru\n\
wdXw02YllJLnTHuwpzIAXl3qTJfpV9jfZFa/axGoMB8I52izBH4SEf0v6qjp97RG\n\
NrPPKjP7BokpjwIBFCQCwkQi40JSAgqJz1JnPpX+tWEp74E9+/wamyh/Kgfsni3G\n\
t9K7EZQBslEi9l7HIcH9D6WCoSrI5z6iImZ3VwIDAQABAoIBAByVvEamlnULHQi3\n\
if0BhvAeBiqyFF8Rc6hgNrL/QBWrMfYf2J6N2bF09+Yt2RII0ZSsHSuEaoXhVffe\n\
FhUzjRKO0Pbrr0Pd7NpL6s/mxr1//LX8dhtPzKJ6Vh+YHcrUR8ep4vUu/0akE11Y\n\
I+1KYZ5mMoi/DTd3m7KQBuqqjjergyRQmtN4Th6JRaPfCkwgfx/6ml49ZIxmFwO/\n\
Sz8idPDIwWSl+hQWqxEiLGnwJml2a3TI1OXC4ViWtTyKyTsQn6eP3pMk7vNDObYV\n\
CVbGtB9kbZILCfpAYSr71VV3ct5+PR58QtaVBPLkE+yKsuqQxGdstamo+MlvaNhq\n\
WhdDSIECgYEA9tK3GpSdBvO6Ctwy15AbWrNZ4j9vp4z1vHqox9Lc6EPnHKAlhrOr\n\
qtnTTRSwlfkZbrS5//pnkh1VfO7C4s1tC60bM6xrX+aXkqo4hq15SNEsHsfEFQl+\n\
EO/wO75o+UapwcP0JmNgKdovbKWGz+GoDiH7mBg2nYamJQljRQ4ABSECgYEAzgv3\n\
Z7GP9ZQcRcyb1eFUW158kTPzBGx9ppjuUGc+HTbBzkCqwutzpfPne74jmWlCCDuI\n\
C/YL3nuu719HuKu0e/6Ho0vUQKyTzv+t+eg9fEzuKj62p2e2hwfkjXBopGdGLftc\n\
nEoG2+2v3fN41747ESsuOxhfdoxedpbqVdCMdXcCgYEAoD8cHAjCjkHs1qIx7Bwm\n\
dlOdFrPi1OBCptToAZwYSj/Tq5UZ6AN8f9ceRZwaLbRlW6fAXr3/QjEDULJfGwsO\n\
Mhd4Yqsdp0y5ucIEIwQ7ixgq0e0WlCXukCaHTPKJitXi3udO+yFnzKRYR+yLwymZ\n\
h1tu0kdeJhXGhqM6rRmyz6ECgYBBBUOwZDcCjxeQ6y1uA+pD3wA+Lf+NMNNB5Fan\n\
5rySKXplJMD+O6DGCL7OrMS9H5snz+lxpNZAAhXFEfMnEoAk9MBGxqIXOp52Hvb+\n\
usvUUs6BZELtduwBlnsIKyXKY5Cg6AZkh3O8qTfrOW6z3iLzmwW+vkU5urgAgkU2\n\
iVYZpwKBgAD+iO+5SVBnDSLWLgOYGE5MBRzDzOT5YfqTkccjqUGdpq0fIzWrX2jT\n\
VgbgY6HcS76Ha2YHJ9EA9MLP+bY1Lfl0IUvOAEDNhTnLADiWQrZH59oR2aido7gP\n\
tPdRbzLQ5HFsfLeL8TeNn50Lt7nde60qq2VYu31YhIZWiRtmdwY5\n\
-----END RSA PRIVATE KEY-----'
        }
    }
    return response


def get_fake_create_thing_response(thingName, thingTypeName):
    thing_arn = THING_ARN_PREFIX + thingName
    return {
        'thingName': thingName,
        'thingArn': thing_arn + thingName,
        'thingId': '70027484-cd47-4aa4-8356-de2636754311'
    }


class MyTestCase(unittest.TestCase):
    def setUp(self) -> None:
        self.mock_iot_client = Mock()
        self.mock_iot_client.create_keys_and_certificate = MagicMock(return_value=get_fake_create_certs_response())
        self.mock_iot_client.create_thing = MagicMock(side_effect=get_fake_create_thing_response)
        self.mock_iot_client.attach_thing_principal = MagicMock(return_value={})
        self.mock_iot_client.attach_policy = MagicMock(return_value={})

    def testCreateCertAndKey(self):
        mock_iot_client = self.mock_iot_client
        thing_name = "Thing123"
        response = createDevice.setup_certs_and_store(mock_iot_client, thing_name)
        mock_iot_client.create_keys_and_certificate.assert_called()

    def testCreateThingWithCert(self):
        mock_iot_client = self.mock_iot_client
        cert_id = get_fake_create_certs_response()['certificateId']
        response = createDevice.create_one_thing_on_iot_with_cert_id(cert_id, mock_iot_client)
        mock_iot_client.create_thing.assert_called()
        mock_iot_client.create_thing.assert_called_with(thingName=createDevice.get_default_thing_name(cert_id),
                                                        thingTypeName=THING_TYPE_NAME)

    def testCreateThingWithThingName(self):
        mock_iot_client = self.mock_iot_client
        thing_name = "Thing12321"
        response = createDevice.create_one_thing_on_iot(thing_name, mock_iot_client)
        mock_iot_client.create_thing.assert_called()
        mock_iot_client.create_thing.assert_called_with(thingName=thing_name,
                                                        thingTypeName=THING_TYPE_NAME)

    def testAttachCertToThing(self):
        mock_iot_client = self.mock_iot_client
        cert_arn = get_fake_create_certs_response()['certificateArn']
        cert_id = get_fake_create_certs_response()['certificateId']
        thing_name = createDevice.get_default_thing_name(cert_id)
        response = createDevice.attach_one_cert_to_thing(thing_name, cert_arn, mock_iot_client)
        mock_iot_client.attach_thing_principal.assert_called()
        mock_iot_client.attach_thing_principal.assert_called_with(thingName=thing_name, principal=cert_arn)

    def testAttachPolicyToCert(self):
        mock_iot_client = self.mock_iot_client
        cert_arn = get_fake_create_certs_response()['certificateArn']
        policy_name = POLICY_NAME
        response = createDevice.attach_one_policy_to_one_cert(policy_name, cert_arn, mock_iot_client)
        mock_iot_client.attach_policy.assert_called()
        mock_iot_client.attach_policy.assert_called_with(policyName=policy_name, target=cert_arn)


class TestUtilFunctions(unittest.TestCase):
    def testGetThingNames(self):
        TEST_THING_NAMES_CSV = "test_thing_names.txt"
        thing_names = createDevice.get_thing_names_from_csv(TEST_THING_NAMES_CSV)
        expected = ["Thing" + str(i) for i in range(5)]
        self.assertListEqual(expected, thing_names)


if __name__ == '__main__':
    unittest.main()
