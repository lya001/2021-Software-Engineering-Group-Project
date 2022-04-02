import json
import boto3

NUMBER_OF_TRACKERS = 10

# pre: device id must end with 4 digits
TRACKER_RANGE = -(10000 // -NUMBER_OF_TRACKERS)

# tracker names related
TRACKER_NAME_PREFIX = "iot-tracker"
TRACKER_NAME_POSTFIX = list(map(str, list(range(NUMBER_OF_TRACKERS))))

"""
>>> event
{
  "payload": {
    "deviceid": "AWS-1234",
    "timestamp": 1604940328,
    "location": {
      "lat": 49.2819,
      "long": -123.1187
    }
  }
}

If your data doesn't match this schema, you can either use the AWS IoT Core rules engine to
format the data before delivering it to this Lambda function, or you can modify the code below to
match it.
"""


def lambda_handler(event, context):
    try:
        device_id = event["payload"]["deviceid"]
        assert isinstance(device_id, str)
        updates = [
            {
                "DeviceId": device_id,
                "SampleTime": event["payload"]["timestamp"],
                "Position": [
                    event["payload"]["location"]["long"],
                    event["payload"]["location"]["lat"]
                ]
            }
        ]
    except AssertionError as e:
        return {
            "statusCode": 400,
            "body": json.dumps({'Error': "Check the types of fields"}, default=str)
        }
    except Exception as e:
        print("Bad event format")
        print("check this field:", e)
        return {
            "statusCode": 400,
            "body": json.dumps({'Check_this_field': e}, default=str)
        }

    client = boto3.client("location")
    tracker_name = get_tracker_name(device_id)

    print("LOG: Tracker: " + tracker_name + ", Updates: " + str(updates))
    response = client.batch_update_device_position(TrackerName=tracker_name, Updates=updates)

    if response['Errors']:
        print(response['Errors'])
        return {
            "statusCode": 400,
            "body": json.dumps({'Errors': [str(response['Errors'])]})
        }
    else:
        return {
            "statusCode": 200,
            "body": json.dumps(response, default=str)
        }


# pre: device id must end with 4 digits
def get_tracker_name(device_id):
    n_device_id = int(device_id[-4:])
    n_tracker_number = n_device_id // TRACKER_RANGE

    return "sim-" + TRACKER_NAME_PREFIX + TRACKER_NAME_POSTFIX[n_tracker_number]