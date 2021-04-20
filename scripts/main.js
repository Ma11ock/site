"use strict";

let stop = false;
let frameCount = 0;
let fps, fpsInterval, startTime, now, then, elapsed;

let canvas;
let context;

// Rows and columns of the board.
let rows = 200;
let cols = 100;

// Divisor for likelyhood the cell starts out as alive.
// For example: 4 means 1/4 chance.
let chance = 4;

// Enum of alive and dead states.
const lifeState = Object.freeze({"alive":1, "dead":2});


// 2D array of cells.
// TODO presets like Gaspar gun 
let cells = (_ => {
  let retVal = [[]];
  for(let i = 0; i < rows; i++) {
    retVal[i] = [];
    for(let j = 0; j < cols; j++) {
      retVal[i][j] = ((Math.floor((Math.random() * chance) + 1) == 1) ? lifeState.alive : lifeState.dead);
    }
  }
  
  return retVal;
})();

let tmpCanvas;
let tmpContext;

function init() {
  let newCanvas = document.createElement('canvas');
  newCanvas.setAttribute("id", 'bkg-canvas');
  newCanvas.style['position'] = 'absolute';
  newCanvas.style['left'] = 0;
  newCanvas.style['right'] = 0;
  newCanvas.style['top'] = 0;
  newCanvas.style['z-index'] = -1;
  newCanvas.width  = window.innerWidth;
  newCanvas.height = window.innerHeight;
  document.body.style.backgroundColor = "black";

  tmpCanvas = document.createElement('canvas');
  tmpContext = tmpCanvas.getContext('2d');
  tmpCanvas.width  = window.innerWidth;
  tmpCanvas.height = window.innerHeight;
  document.body.style.backgroundColor = "black";


  document.body.appendChild(newCanvas);
  
  
  canvas = document.getElementById('bkg-canvas');
  context = canvas.getContext('2d');
}


// get the total number of neighboring cells that are alive.
function totalAliveNeighbors(x, y)
{
  let sum = 0;
  let newX = 0;
  let newY = 0;
  for(let i = -1; i < 2; i++)
  {
    for(let j = -1; j < 2; j++)
    {
      newX = (x + i + rows) % rows;
      newY = (y + j + cols) % cols;
      if(cells[newX][newY] == lifeState.alive)
        sum++;
    }
  }

  if(cells[x][y] == lifeState.alive)
    sum--;
  
  return sum;    
}

// Update the cells.
function updateCells()
{
  // Deep copy the board.
  let nextBoard = JSON.parse(JSON.stringify(cells));

  // Update the new board.
  for(let i = 0; i < rows; i++)
  {
    for(let j = 0; j < cols; j++)
    {
      let numAlive = totalAliveNeighbors(i, j);
      let isAlive = (cells[i][j] == lifeState.alive);
      
      if(isAlive && (numAlive < 2 || numAlive > 3))
        nextBoard[i][j] = lifeState.dead;
      else if(!isAlive && numAlive == 3)
        nextBoard[i][j] = lifeState.alive;
    }
  }
  cells = nextBoard;
}



function startAnimating(fps) {
  fpsInterval = 1000 / fps;
  then = Date.now();
  startTime = then;
  console.log("Starting on Unix timestamp: " + startTime);
  animate();
}

// Optimized drawing function.
function draw() {
  let wWidth = window.innerWidth;
  let wHeight = window.innerHeight;
  let drawCell = { 'x' : 0, 'y' : 0, 'width' : wWidth / rows, 'height' : wHeight / cols};

  tmpContext.clearRect(0, 0, tmpCanvas.width, tmpCanvas.height);
  context.clearRect(0, 0, canvas.width, canvas.height);
  
  tmpContext.beginPath();
  tmpContext.fillStyle = '#FFFFFF';
  for(let i = 0; i < rows; i++) {
    for(let j = 0; j < cols; j++) {
      drawCell.x = i * drawCell.width;
      drawCell.y = j * drawCell.height;

      if(cells[i][j] === lifeState.alive)
        tmpContext.rect(drawCell.x, drawCell.y, drawCell.width, drawCell.height);
    }
  }
  tmpContext.fill();
  tmpContext.closePath();

  context.drawImage(tmpCanvas, 0, 0, wWidth, wHeight);
}

function animate() {

  // stop
  if (stop) {
    return;
  }

  updateCells();
  
  // request another frame

  requestAnimationFrame(animate);

  // calc elapsed time since last loop

  now = Date.now();
  elapsed = now - then;

  // if enough time has elapsed, draw the next frame

  if (elapsed > fpsInterval) {

    // Get ready for next frame by setting then=now, but...
    // Also, adjust for fpsInterval not being multiple of 16.67
    then = now - (elapsed % fpsInterval);
    draw();


    // TESTING...Report #seconds since start and achieved fps.
    let sinceStart = now - startTime;
    let currentFps = Math.round(1000 / (sinceStart / ++frameCount) * 100) / 100;
    context.fillStyle = 'white';
    context.fillRect(0, 0, 200, 100);
    context.font = '25px Arial';
    context.fillStyle = 'black';
    context.fillText("FPS: " + currentFps, 10, 30);

  }
}


init();
startAnimating(20);
