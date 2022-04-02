import json
import random

GENERATED_SENT_LOGS = "./generated_simulation_log"
GENERATED_RECEIVE_LOGS = "./generated_receive_log"


def generate_device_ids(num_devices):
    base_name = "AWS-"
    suffix_length = 4
    name_suffixes = [str(i).zfill(suffix_length) for i in range(num_devices)]
    device_ids = [base_name + name_suffix for name_suffix in name_suffixes]
    return device_ids


def generate_entries_for_one_device(device_id, base_timestamp, base_position, num_entries):
    timestamps = [base_timestamp + 50 * i for i in range(num_entries)]
    positions = [[base_position[0] + i * 1e-5, base_position[1] + i * 1e-5] for i in
                 range(num_entries)]

    device_history = [{"id": device_id, "time": timestamps[i], "coordinates": positions[i]} for i in range(num_entries)]
    return device_history


def generate_sent_logs(num_devices, num_entries):
    filename = GENERATED_SENT_LOGS
    base_timestamp = 1641623437517
    base_position = [-86.77440982202124, 36.129015825214594]
    all_entries = generate_all_log_entries(base_position, base_timestamp, num_devices, num_entries)
    write_all_entries_to_logfile(filename, all_entries)


def generate_receive_logs(num_devices, num_entries):
    filename = GENERATED_RECEIVE_LOGS
    base_timestamp = 1641623437517 + 200
    base_position = [-86.77440982202124, 36.129015825214594]
    all_entries = generate_all_log_entries(base_position, base_timestamp, num_devices, num_entries)
    double_all_entries = all_entries * 2
    random.shuffle(double_all_entries)
    write_all_entries_to_logfile(filename, double_all_entries)


def generate_all_log_entries(base_position, base_timestamp, num_devices, num_entries):
    device_ids = generate_device_ids(num_devices)
    all_entries = []

    for i, device_id in enumerate(device_ids):
        one_device_entries = generate_entries_for_one_device(device_id, base_timestamp + i, base_position, num_entries)
        all_entries.extend(one_device_entries)
    return all_entries


def write_all_entries_to_logfile(filename, all_entries):
    with open(filename, 'w') as file:
        for entry in all_entries:
            json.dump(entry, file)
            file.write('\n')


def generate_sent_and_receive_logs():
    num_devices = 1000
    num_entries = 20
    generate_sent_logs(num_devices, num_entries)
    generate_receive_logs(num_devices, num_entries)


def main():
    generate_sent_and_receive_logs()


if __name__ == '__main__':
    main()
