import registerServiceWorker from './registerServiceWorker';
import React from 'react'
import ReactDOM from 'react-dom'

import {createStore, combineReducers, applyMiddleware} from 'redux'
import {Provider} from 'react-redux'

import createHistory from 'history/createBrowserHistory'
import {Router} from 'react-router-dom'

import {routerReducer, routerMiddleware} from 'react-router-redux'

import reducer from './reducers/index'
import App from './App'

const history = createHistory()

const middleware = routerMiddleware(history)

const store = createStore(combineReducers({
    ...reducer,
    router: routerReducer
}), applyMiddleware(middleware))

ReactDOM.render(
    <Provider store={store}>
    <Router history={history}>
        <App/>
    </Router>
</Provider>, document.getElementById('root'))
registerServiceWorker();