import {makeStyles} from '@material-ui/core/styles'

const useStyles = makeStyles((theme) => ({
  map: {
    width: '100vw',
    [theme.breakpoints.down('md')]: {
      height: '64vh'
    },
    [theme.breakpoints.up('md')]: {
      height: '100vh'
    },
    padding: theme.spacing(1),
  },
  info: {
    height: '60%',
    width: '100%',
    padding: theme.spacing(1),
  },
  addTracker: {
    height: "40%",
    padding: theme.spacing(1),
  },
  paper: {
    height: '100%',
    padding: theme.spacing(1.8, 1.8, 1.8, 1.8),
    overflowY: "auto"
  },
}))

export default useStyles