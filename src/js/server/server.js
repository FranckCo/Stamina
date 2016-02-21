import express from 'express';

const SERVER_PORT = 3000;

var server = express();

server.get('/', function (request, response) {
  response.send('Hello World!');
});

server.listen(SERVER_PORT, () => {
  console.log(`Server is now running on http://localhost:${SERVER_PORT}`);
});
