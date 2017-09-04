import {combineReducers} from 'redux'

const env = (state = 'dev', action) => state
const addTodo = (state = 'hi', action) => {
    switch (action.type) {
        case 'ADD_TODO':
            return action.text
        default:
            return state
    }
}

export default combineReducers({env, addTodo})