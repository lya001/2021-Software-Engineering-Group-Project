from iot_to_tracker_lambda import *
import boto3
import json


def main():
    # open json file
    file = open('message.json')

    # returns json object as dictionary
    message = json.load(file)
    print('Event:')
    print(message)

    # publishing message to tracker
    result = lambda_handler(message, None)
    status_code = result['statusCode']
    if status_code != 200:
        print('Publishing Errors:')
        print(result['body'])
    else:
        print("Successful upload")
        print(result['body'])

    # receiving tracker location
    client = boto3.client("location")
    device_id = message['payload']['deviceid']
    tracker_name = get_tracker_name(device_id)
    response2 = client.get_device_position(TrackerName=tracker_name, DeviceId=device_id)
    status_code = response2['ResponseMetadata']['HTTPStatusCode']
    if status_code == 200:
        print("got from tracker: ")
        print(
            f"Device: {response2['DeviceId']} "
            f"at {response2['Position']}, "
            f"Received time: {response2['ReceivedTime']}, "
            f"SampleTime: {response2['SampleTime']}")
        loc = message['payload']['location']
        print("Expected device position:",  f"[{loc['long']}, {loc['lat']}]")
    else:
        print("Error from tracker")
        print(response2['Errors'])

    # close file
    file.close()


if __name__ == '__main__':
    main()
