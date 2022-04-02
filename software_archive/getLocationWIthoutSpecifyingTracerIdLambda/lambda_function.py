import json
import requests

NUMBER_OF_TRACKERS = 10

# pre: device id must end with 4 digits
TRACKER_RANGE = -(10000 // -NUMBER_OF_TRACKERS)

# tracker names related
TRACKER_NAME_PREFIX = "iot-tracker"
TRACKER_NAME_POSTFIX = list(map(str, list(range(NUMBER_OF_TRACKERS))))
SIMULATION_NAME_PREFIX = "sim-"

# pre: device id must end with 4 digits


def get_tracker_name(device_id):
    n_device_id = int(device_id[-4:])
    n_tracker_number = n_device_id // TRACKER_RANGE

    return SIMULATION_NAME_PREFIX + TRACKER_NAME_PREFIX + TRACKER_NAME_POSTFIX[n_tracker_number]


def lambda_handler(event, context):
    device_id = event['queryStringParameters']['deviceid']
    
    try:
        tracker_name = get_tracker_name(device_id)
        data = requests.get(f'https://xvojnsbpvd.execute-api.eu-west-1.amazonaws.com/default/getLocation?type=device&trackername={tracker_name}&deviceid={device_id}').json()
    except Exception:
        tracker_name = ''
        print(f'No such entry with device_id {device_id}')
    return {
        'headers': {
            "Access-Control-Allow-Headers": "Content-Type",
            "Access-Control-Allow-Origin": "*",
            "Access-Control-Allow-Methods": "*",
            "Access-Control-Allow-Credentials": "true"
        },
        'statusCode': 200,
        'body': json.dumps({'TrackerName': tracker_name})
    }