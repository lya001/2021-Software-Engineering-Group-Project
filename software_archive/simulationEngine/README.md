# Publishing to IoT and Trackers

Prerequisite: aws credentials under ~/.aws/

Create iot Things:
  Setup python:
    python -m venv ./venv
    . ./venv/scripts/activate
    pip install -r requirements.txt
    python createDevice.py