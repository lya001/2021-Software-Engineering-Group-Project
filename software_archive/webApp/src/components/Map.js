import {Checkbox, Container, Grid, Paper, Typography} from "@mui/material";
import useStyles from "../styles/MapPageStyles";
import {MapContainer, Marker, Popup, TileLayer} from 'react-leaflet'
import {useEffect, useState} from "react";
import axios from "axios";
import {DeviceTypeSelector} from "./DeviceTypeSelector";

function Map(props) {

  const classes = useStyles()

  const [deviceToTrackerMap, setDeviceToTrackerMap] = useState({})

  const [deviceMap, setDeviceMap] = useState({})
  const [deviceWithoutTrackerMap, setDeviceWithoutTrackerMap] = useState({})
  const devicesWithoutTrackers = useState({ids: []})

  const [active, setActive] = useState({})

  useEffect(() => {

    // Store in temp all active devices that have been requested without specifying a tracker
    let temp = {}
    Object.keys(deviceWithoutTrackerMap).forEach(deviceId => {
      if (devicesWithoutTrackers[0].ids.some(id => id === deviceId)) {
        temp[deviceId] = deviceWithoutTrackerMap[deviceId]
      }
    })

    // If a tracker is removed, also remove all the corresponding displayed devices
    for (const device of Object.keys(deviceMap)) {
      if (props.trackerIds[0].ids.every(trackerId => trackerId !== deviceToTrackerMap[device])) {
        setDeviceMap(prevState => {
          const temp = prevState
          delete temp[device]
          return temp
        })
      }
    }

    const interval = setInterval(
      (async () => {
        // Make sure all the displayed devices have been requested by the user
        setDeviceWithoutTrackerMap(temp)

        for (const id of props.trackerIds[0].ids) {
          const response = await axios.get(`https://xvojnsbpvd.execute-api.eu-west-1.amazonaws.com/default/getLocation?type=list&trackername=${id}`)
          if (response.data === "An error occurred") {
            return
          }

          response.data.Entries.forEach((entry) => {
            // Remove all displayed devices for which the corresponding tracker has been removed by the user
            if (typeof deviceToTrackerMap[entry.DeviceId] === "undefined") {
              setDeviceToTrackerMap(prevState => ({
                ...prevState,
                [entry.DeviceId]: id
              }))
            }

            // If the device has a position, update the values
            if (typeof entry.Position != 'undefined') {
              const position = entry.Position
              const sampleTime = entry.SampleTime
              setDeviceMap(prevValue => {
                return {
                  ...prevValue,
                  [entry.DeviceId]: {
                    position,
                    sampleTime
                  }
                }
              })
            }
          })
        }

        async function fetchDeviceLocation(id) {
          // Deduce the tracker id from the name of the device
          const deducedTrackerId = await axios.get(`https://1tfpn0ogq0.execute-api.eu-west-1.amazonaws.com/default/get-device-without-tracker?deviceid=${id}`)
          const trackerName = deducedTrackerId.data.TrackerName

          if (props.trackerIds[0].ids.some(id => id === trackerName) || trackerName === "") {
            // If the deduced tracker id is already tracked by the user, it means that the device is already displayed
            return false
          }

          const response = await axios.get(`https://xvojnsbpvd.execute-api.eu-west-1.amazonaws.com/default/getLocation?type=device&trackername=${trackerName}&deviceid=${id}`)

          // If the device has a position, update the values
          if (typeof response.data.Position !== 'undefined') {
            const position = response.data.Position
            const sampleTime = response.data.SampleTime
            setDeviceWithoutTrackerMap(prevValue => {
              return {
                ...prevValue,
                [response.data.DeviceId]: {
                  position,
                  sampleTime
                }
              }
            })
          }
          return true
        }

        // Update the position of all the devices that have been requested without a tracker
        for (const id of devicesWithoutTrackers[0].ids) {
          if (!await fetchDeviceLocation(id)) {
            console.log("Deleted: ", id)
            devicesWithoutTrackers[1](prevState => ({
              ids: prevState.ids.filter(cid => cid !== id)
            }))
          }
        }
      }), 1000)
    return () => {
      clearInterval(interval);
    }
  }, [deviceMap, props.trackerIds, devicesWithoutTrackers, deviceWithoutTrackerMap, deviceToTrackerMap])

  // Function to display all device locations on a map, using pointers
  function displayPointers(locationsMap) {
    return Object.keys(locationsMap).map((deviceId, i) => {
      if (typeof active[deviceId] == "undefined" || active[deviceId]) {
        return <Marker key={i}
                       position={locationsMap[deviceId].position}>
          <Popup key={i}> Device: {deviceId} </Popup> </Marker>
      }
      return <></>
    })
  }

  // Function to display the tracked device IDs in the right window
  function displayDeviceIds(deviceMap) {
    return Object.keys(deviceMap).map((deviceId, i) => {
      function handleCheckboxChange() {
        setActive(prevState => {
          return {
            ...prevState,
            [deviceId]: typeof active[deviceId] == "undefined" ? false : !active[deviceId]
          }
        })
      }

      return <Container key={i} style={{
        display: "flex",
        width: "100%",
        padding: "0px",
        margin: "0px 0px 8px 0px"
      }}>
        <Checkbox defaultChecked onChange={handleCheckboxChange} style={{padding: "0px"}}/>
        <Typography variant='h6'
                    style={{
                      wordWrap: "break-word",
                      width: "100%",
                      marginLeft: "4px",
                      padding: "0px"
                    }}> {deviceId}: {Number.parseFloat(deviceMap[deviceId].position[0]).toFixed(4)} {Number.parseFloat(deviceMap[deviceId].position[1]).toFixed(4)} </Typography>
      </Container>
    })
  }

  return (
    <>
      <Grid container alignItems="center" justifyContent="center">
        <Grid item xs={12} md={9} className={classes.map}>
          {(() => {
            return (
              <Paper className={classes.paper}
                     elevation={3}>
                <MapContainer center={[36.16, -86.79]} zoom={13} scrollWheelZoom
                              style={{width: '100%', height: '100%'}}>
                  <TileLayer
                    attribution='&copy; <a href="https://osm.org/copyright">OpenStreetMap</a> contributors'
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                  />
                  {displayPointers(deviceMap)}
                  {displayPointers(deviceWithoutTrackerMap)}
                </MapContainer>
              </Paper>
            )
          })()}
        </Grid>
        <Grid item md={3} sx={{height: '100vh', width: '100%'}}>
          <Grid direction="row" rowSpacing={0} container sx={{height: '100vh', width: '100%'}}>
            <Grid item
                  xs={12}
                  className={classes.info}>
              <Paper className={classes.paper}
                     elevation={3}>
                {Object.keys(deviceMap).length !== 0 ? <Typography variant="h5" gutterBottom> Tracker Devices: </Typography> : <></>}
                {displayDeviceIds(deviceMap)}
                {Object.keys(deviceWithoutTrackerMap).length !== 0 ? <Typography variant="h5" gutterBottom> Standalone Devices: </Typography> : <></>}
                {displayDeviceIds(deviceWithoutTrackerMap)}
              </Paper>
            </Grid>
            <Grid item
                  xs={12}
                  className={classes.addTracker}>
              <DeviceTypeSelector classes={classes} trackerIds={props.trackerIds} deviceIds={devicesWithoutTrackers}
                                  label="Type"
                                  menuItems={["Tracker", "Device"]}
              />
            </Grid>
          </Grid>
        </Grid>
      </Grid>
    </>
  );
}

export default Map;
