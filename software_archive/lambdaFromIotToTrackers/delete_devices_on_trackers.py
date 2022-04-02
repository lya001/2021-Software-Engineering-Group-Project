import boto3

TRACKER_NAME_PREFIX = "iot-tracker"


def main():
    tracker_names = [TRACKER_NAME_PREFIX + str(i) for i in range(10)]
    client = boto3.client("location")
    return

    for tracker_name in tracker_names:
        # Delete devices on this tracker
        device_ids_on_this_tracker = []
        response = client.list_device_positions(TrackerName=tracker_name)
        entries = response['Entries']
        for entry in entries:
            device_ids_on_this_tracker.append(entry['DeviceId'])
        print("now trying to delete", device_ids_on_this_tracker)
        response = client.batch_delete_device_position_history(TrackerName=tracker_name,
                                                               DeviceIds=device_ids_on_this_tracker)
        print(response)


if __name__ == '__main__':
    main()
