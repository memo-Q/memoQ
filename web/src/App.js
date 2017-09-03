import React, {Component} from 'react';
import {Route, Link} from 'react-router-dom'

const About = () => (
  <div>
    <h2>About</h2>
  </div>
)

const Home = () => (
  <div>
    <h2>Home</h2>
  </div>
)
class App extends Component {
  render() {
    return (
      <div>
        <ul>
          <li>
            <Link to="/">Home</Link>
          </li>
          <li>
            <Link to="/about">About</Link>
          </li>
        </ul>
        <Route exact path="/" component={Home}/>
        <Route path="/about" component={About}/>
      </div>
    )
  }
}

export default App;