let canvas: HTMLCanvasElement;
let context: CanvasRenderingContext2D;

let bkgCallback: (canvasCtx: HTMLCanvasElement,
                  contex2d: CanvasRenderingContext2D) => void;

export function initBackground() {
    let newCanvas: HTMLCanvasElement = document.createElement('canvas');
    newCanvas.setAttribute("id", 'bkg-canvas');
    newCanvas.style['position'] = 'fixed';
    newCanvas.style['left'] = '0';
    newCanvas.style['right'] = '0';
    newCanvas.style['top'] = '0';
    newCanvas.style.zIndex = '-100';
    newCanvas.width  = window.innerWidth;
    newCanvas.height = window.innerHeight;

    document.body.appendChild(newCanvas);

    canvas = <HTMLCanvasElement>document.getElementById('bkg-canvas');
    context = canvas.getContext('2d');
}

function doBackground() {
    context.clearRect(0, 0, canvas.width, canvas.height);

    bkgCallback(canvas, context);

    context.fill();
    context.closePath();
    context.drawImage(canvas, 0, 0, canvas.width, canvas.height);
}

export function setBkgFunc(callback: (canvasCtx: HTMLCanvasElement,
                                      contex2d: CanvasRenderingContext2D) => void) {
    bkgCallback = callback;
    doBackground();
}

window.addEventListener('resize', _ => {
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
    doBackground();
});
