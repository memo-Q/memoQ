import {combineReducers} from 'redux'

const env = (state = 'dev', action) => state

const reducer = combineReducers({env})

export default reducer