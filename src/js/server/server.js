import express from 'express';
import path from 'path';

const SERVER_PORT = 3000;

var server = express();

server.get('/build/build.min.js', function (request, response) {
  response.sendFile(path.resolve(__dirname, '../../../dist/build/build.min.js'));
});

server.get('*', function (request, response) {
  response.sendFile(path.resolve(__dirname, '../../../dist/index.html'));
});

server.listen(SERVER_PORT, () => {
  console.log(`Server is now running on http://localhost:${SERVER_PORT}`);
});
