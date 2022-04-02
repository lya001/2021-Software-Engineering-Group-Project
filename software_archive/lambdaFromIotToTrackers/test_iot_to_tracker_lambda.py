import datetime
import unittest

import iot_to_tracker_lambda
import boto3


def get_current_utc0_time():
    return datetime.datetime.now().timestamp()


class TestLambda(unittest.TestCase):
    def test_device_ATE_1024_will_go_to_tracker1(self):
        device_id = "ATE-1024"
        result = iot_to_tracker_lambda.get_tracker_name(device_id)
        answer = "sim-iot-tracker1"
        self.assertEqual(answer, result)

    def test_device_AWS_7686_will_go_to_tracker7(self):
        device_id = "AWS-7686"
        result = iot_to_tracker_lambda.get_tracker_name(device_id)
        answer = "sim-iot-tracker7"
        self.assertEqual(answer, result)


class TestInteractionWithLocationClient(unittest.TestCase):
    def test_event_will_be_handled_tracker0(self):
        device_id = 'ATE-0124'
        event = {'payload': {'deviceid': device_id, 'timestamp': get_current_utc0_time(),
                             'location': {'lat': 49.2819, 'long': -123.1187}}}
        result = iot_to_tracker_lambda.lambda_handler(event, None)

        print(result)

    def test_event_will_be_handled_tracker1(self):
        device_id = 'AWS-1999'
        event = {'payload': {'deviceid': device_id, 'timestamp': get_current_utc0_time(),
                             'location': {'lat': 49.2819, 'long': -123.1187}}}
        result = iot_to_tracker_lambda.lambda_handler(event, None)

        print(result)

    def test_get_from_trackers(self):
        TRACKER_0 = "sim-iot-tracker0"
        TRACKER_1 = "sim-iot-tracker1"
        device_ids = ["device" + str(i) for i in range(0, 200, 50)]
        client = boto3.client("location")
        response1 = client.batch_get_device_position(TrackerName=TRACKER_0, DeviceIds=device_ids)
        response2 = client.batch_get_device_position(TrackerName=TRACKER_1, DeviceIds=device_ids)
        print(response1['DevicePositions'])
        print(response2['DevicePositions'])
