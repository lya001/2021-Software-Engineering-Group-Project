import {Button, FormControl, Grid, InputLabel, MenuItem, Paper, Select, TextField, Typography} from "@mui/material";
import {useState} from "react";

export function DeviceTypeSelector(props) {

  const [objectId, setObjectId] = useState("")
  const [deviceType, setDeviceType] = useState(0)

  function handleDeviceTypeChange(event) {
    setDeviceType(event.target.value);
  }

  function handleObjectIdChange(event) {
    setObjectId(event.target.value)
  }

  // Handle the event of adding a device (either a tracker, or a single device)
  function handleObjectAddition() {
    if (deviceType === 0) { // tracker
      if (!props.trackerIds[0].ids.some(id => id === objectId)) {
        props.trackerIds[1](prevState => ({
          ids: [...prevState.ids, objectId]
        }))
      }
    } else { // device
      if (!props.deviceIds[0].ids.some(id => id === objectId)) {
        props.deviceIds[1](prevState => ({
          ids: [...prevState.ids, objectId]
        }))
      }
    }
    setObjectId("")
  }

  // Handle the event of removing a device (either a tracker, or a single device)
  function handleObjectRemoval() {
    if (deviceType === 0) { // tracker
      props.trackerIds[1](prevState => ({
        ids: prevState.ids.filter(id => id !== objectId)
      }))
    } else { // device
      props.deviceIds[1](prevState => ({
        ids: prevState.ids.filter(id => id !== objectId)
      }))
    }
    setObjectId("")
  }

  return <Paper className={props.classes.paper}
                elevation={3}>
    <Typography variant="h5" style={{marginBottom: "24px"}}> Add or remove an object: </Typography>
    <FormControl fullWidth>
      <InputLabel> {props.label} </InputLabel>
      <Select
        value={deviceType}
        label={props.label}
        onChange={handleDeviceTypeChange}
      >
        {props.menuItems.map((item, i) => <MenuItem value={i} key={i}> {item} </MenuItem>)}
      </Select>
      <TextField onChange={handleObjectIdChange} value={objectId} variant="outlined"
                 label={`${props.menuItems[deviceType]} ID`} style={{margin: "16px 0px"}}/>
      <Grid container>
        <Grid item xs={6} style={{paddingRight: "8px"}}>
          <Button fullWidth variant="contained" onClick={handleObjectAddition}>
            Add
          </Button>
        </Grid>
        <Grid item xs={6} style={{paddingLeft: "8px"}}>
          <Button fullWidth variant="contained" color="error" onClick={handleObjectRemoval}>
            Remove
          </Button>
        </Grid>
      </Grid>
    </FormControl>
  </Paper>;
}