// Cropyright (C) Ryan Jeffrey 2022
// A simple express server that uses handlebars.
import express from 'express';
import path from 'path';
import exphbs, { engine } from 'express-handlebars';
import fs from 'fs';

const app = express();
const port = process.env.PORT || 3000;
let theBkgScript = '';
let allBkgScripts: string[] = [];

function getBkgScripts(scriptDir: string) : string[] {
    let result: string[] = [];
    let files = fs.readdirSync(scriptDir);
    files.forEach(file => {
        if(path.extname(file) == '.js' && file != 'backs.js') {
            result.push(file);
        }
    });
    return result;
}

allBkgScripts = getBkgScripts('./external/site-bkgs/bin/');
theBkgScript = allBkgScripts[Math.floor(Math.random() * allBkgScripts.length)];

// Get a new background script once per day.
setInterval(() => {
    theBkgScript = allBkgScripts[Math.floor(Math.random() * allBkgScripts.length)];
}, (1000 * 60 * 60 * 24));

function getMonthByNumber(i: number) : string {
    const months = [ 'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul',
                     'Aug', 'Sep', 'Oct', 'Nov', 'Dec' ];
    return (i in months) ? months[i] : "";
}

function permissionToString(i: number) : string {
    // Unix file permission array. The mode is the index in the array.
    const permStrings = ['---', '--x', '-w-', '-wx', 'r--', 'r-x', 'rw-', 'rwx'];
    return (i in permStrings) ? permStrings[i] : "";
}

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
        this.mtime = LSStat.lsTime(stats.mtimeMs);
        this.basename = thePath;
    }

    // Get the mtime in the same format that LS would.
    static lsTime(timeMS: number) : string {
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

    static lsList(theDir: string, ext: string, files: string[]) : LSStat[] {
        let fileStats: LSStat[] = [];
        files.forEach((element: string) => {
            fileStats.push(new LSStat(path.join(theDir, element + ext)));
        });
        return fileStats;
    }

    static lsDir(thePath: string) : LSStat[] {
        let fileStats: LSStat[] = [];
        // TODO error checking.
        let files = fs.readdirSync(thePath);

        files.forEach((file) => {
            fileStats.push(new LSStat(file));
        });

        return fileStats;
    }

    static fileExistsIn(thePath: string, statList: LSStat[]) : boolean {
        for(let i = 0; i < statList.length; i++) {
            if(statList[i].basename == thePath)
                return true;
        }
        return false;
    }
}

// Dummy class just for inheritance.
class Command {
    args: string;
    constructor(args: string) {
        this.args = args;
    }
}

class LS extends Command {
    lsList: LSStat[];

    constructor(dir: string, ext:string, names: string[]) {
        super(dir);
        this.lsList = LSStat.lsList(dir, ext, names);
    }
}

class Cat extends Command {
    markup: string;

    constructor(path: string) {
        super(path);
        this.markup = fs.readFileSync(path, 'utf8');
    }
}

class TerminalWindow {
    commands: Command[];

    constructor(...commands: Command[]) {
        this.commands = commands;
    }
}

// App config

app.engine('handlebars', engine({ defaultLayout: 'main' }));
app.set('view engine', 'handlebars');
app.set('views', "./views");

app.use(express.static(path.join(process.cwd(), 'public')));
app.use(express.static(path.join(process.cwd(), 'external')));
app.use(express.json());

// TODO maybe a system that exports org to handlebars.
const postItems = LSStat.lsDir('posts');
// Get the requested post
app.get('/posts/:post', async (req, res, next) => {
    let post = req.params.post.toLowerCase();
    if(LSStat.fileExistsIn(post, postItems)) {
        await fs.readFile(post, 'utf8', (err, data) => {
            if(err) {
                console.log("Error when looking for post", post);
                // TODO internal error.
                res.status(404).render('404');
            }
            else {
                res.status(200).render('writing', { text : data });
            }
        });
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
// Generate files object.
const files = LSStat.lsDir('public/files');
// Server entry for files.
app.get('/files', (req, res, next) => {
    res.status(200).render('files', {
        entries: files
    });
});

// LS everything.
const frontPageItems = LSStat.lsList('.', '.html', ['main', 'software', 'sneed']);

app.get('/bkgs', (req, res, next) => {
    res.status(200).render('index', {
        windows: [new TerminalWindow(new Cat('public/bkgs/index.html'))],
        bkgScript: `/site-bkgs/bin/${theBkgScript}`,
    });
});

app.get('/bkgs/:item', async (req, res, next) => {
    let scriptPath = path.join(process.cwd(),
                               `external/site-bkgs/bin/${req.params.item}.js`);
    await fs.exists(scriptPath, (exists) => {
        if(exists) {
            res.set('Content-Type', 'text/html');
            res.status(200).send(Buffer.from(
                `<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="en" xml:lang="en">
<body>
<script defer type="module" src="/site-bkgs/bin/${req.params.item}.js"></script>
</body>
</html>
`));
        }
        else {
            res.status(404).render('404');
        }
    });
});

app.get('/:item', async (req, res, next) => {
    let item = req.params.item.toLowerCase();
    if(LSStat.fileExistsIn(item, frontPageItems)) {
        await fs.readFile(item, 'utf8', (err, buf) => {
            if(err) {
                console.log("Error when looking for item", item);
                // TODO internal error.
                res.status(404).render('404');
            }
            else {
                res.status(200).render('post', { text : fs.readFileSync(item) });
            }
        });
    }
    else {
        // Page not found.
        res.status(404).render('404');
    }
});

app.get('/', (req, res, next) => {
    // TODO cache.
    res.status(200).render('index', {
        windows: [new TerminalWindow(new Cat('public/figlet.html'),
                                     new LS('.', '.html', ['main', 'software', 'sneed']),
                                     new Cat('public/front.html'))],
        bkgScript: `/site-bkgs/bin/${theBkgScript}`,
    });
});

// 404 is last.
app.get('*', (req, res) => {
    res.status(404).render('404');
});

// Server initialize.
app.listen(port, () => {
    console.log('== Server is listening on port', port,
                'in current directory', process.cwd());
});
