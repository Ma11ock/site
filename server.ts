// Cropyright (C) Ryan Jeffrey 2022
// A simple express server that uses handlebars.

import express from 'express';
import path from 'path';
import exphbs, { engine } from 'express-handlebars';
import fs from 'fs';

const app = express();
const port = process.env.PORT || 3000;

class LSStat {
    perms: string;
    numLinks: number;
    fileSize: number;
    mtime: string;
    basename: string;
    
    constructor(thePath: string) {
        let stats = fs.statSync(thePath);

        // ls file permissions. Convert into an easier format to use.
        if(!stats) {
            console.error("Could not stat", thePath);
            return;
        }
        // Convert mode to string.
        let unixFilePermissions = (stats.mode & parseInt('777', 8)).toString(8);
        let permsResult = permissionToString(parseInt(unixFilePermissions[0]));
        permsResult += permissionToString(parseInt(unixFilePermissions[1]));
        permsResult += permissionToString(parseInt(unixFilePermissions[2]));

        let prefixChar = '-';
        if(stats.isDirectory()) {
            prefixChar = 'd';
        }

        this.perms = `${prefixChar}${permsResult}`;
        this.numLinks = stats.nlink;
        this.fileSize = stats.size;
        this.mtime = lsTime(stats.mtimeMs);
        this.basename = path.parse(path.basename(thePath)).name;
    }
}

function getMonthByNumber(i: number) : string {
    const months = [ 'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul',
                     'Aug', 'Sep', 'Oct', 'Nov', 'Dec' ];
    return (i in months) ? months[i] : "";
}

// Get the mtime in the same format that LS would.
function lsTime(timeMS: number) : string {
    let fileDate = new Date(timeMS);
    let addString = "";
    // If the file was updated this year then set the last column to the
    // hour and minute. Else, the last column should be the year.
    if((new Date()).getFullYear() != fileDate.getFullYear())
        addString = `${fileDate.getHours()}:${fileDate.getMinutes()}`;
    else
        addString = ` ${fileDate.getFullYear()}`;
    return `${getMonthByNumber(fileDate.getMonth())} ${fileDate.getDate()}  ${addString}`;
}

function permissionToString(i: number) : string {
    // Unix file permission array. The mode is the index in the array.
    const permStrings = ['---', '--x', '-w-', '-wx', 'r--', 'r-x', 'rw-', 'rwx'];
    return (i in permStrings) ? permStrings[i] : "";
}


function lsList(theDir: string, ext: string, ...files: string[]) : LSStat[] {
    let fileStats: LSStat[] = [];
    files.forEach((element: string) => {
        fileStats.push(new LSStat(path.join(theDir, element + ext)));
    });
    return fileStats;
}

function lsDir(thePath: string) : LSStat[] {
    let fileStats: LSStat[] = [];
    fs.readdir(thePath, (err, files) => {
        if(err) {
            return console.error('Cannot scane directory ', thePath, ": ", err);
        }

        files.forEach((file) => {
            fileStats.push(new LSStat(file));
        });
    });

    return fileStats;
}

// App config

app.engine('handlebars', engine({ defaultLayout: 'main' }));
app.set('view engine', 'handlebars');
app.set('views', "./views");

app.use(express.static(path.join(__dirname, 'public')));
app.use(express.json());

// TODO maybe a system that exports org to handlebars.

// Get the requested post
app.get('/posts/:post', (req, res, next) => {
    let post = req.params.post.toLowerCase();
    if(fs.existsSync(post)) {
        res.status(200).render('writing', { text : fs.readFileSync(post) });
    }
    else {
        // Page not found.
        res.status(404).render('404');
    }
});
// Posts index file.
app.get('/posts', (req, res, next) => {
    res.status(200).render('indexWriting');
});
// index.html should be before 404 and after everything else


app.get('/', (req, res, next) => {
    let test = lsList('public', '.html', 'main', 'software', 'sneed');
    res.status(200).render('index', {
        entries: test
    });
});

// 404 is last.
app.get('*', (req, res) => {
    res.status(404).render('404');
});

app.listen(port, () => {
    console.log('== Server is listening on port', port);
});
