// Cropyright (C) Ryan Jeffrey 2022
// A simple express server that uses handlebars.


// TODO convert to typescript

const path = require('path');
const express = require('express');
const { engine } = require('express-handlebars');

const fs = require('fs');

var app = express();
var port = process.env.PORT || 3000;
var exphbs = require('express-handlebars');

function getMonthByNumber(i) {
  const months = [ 'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul',
                   'Aug', 'Sep', 'Oct', 'Nov', 'Dec' ];
  return (i in months) ? months[i] : null;
}

// Get the mtime in the same format that LS would.
function lsTime(timeMS) {
  let fileDate = new Date(timeMS);
  console.log(fileDate, timeMS);
  let addString = "";
  // If the file was updated this year then set the last column to the
  // hour and minute. Else, the last column should be the year.
  if((new Date()).getFullYear() != fileDate.getFullYear())
    addString = `${fileDate.getHours()}:${fileDate.getMinutes()}`;
  else
    addString = ` ${fileDate.getFullYear()}`;
  return `${getMonthByNumber(fileDate.getMonth())} ${fileDate.getDate()}  ${addString}`;
}

function permissionToString(i) {
  // Unix file permission array. The mode is the index in the array.
  const permStrings = ['---', '--x', '-w-', '-wx', 'r--', 'r-x', 'rw-', 'rwx'];
  return (i in permStrings) ? permStrings[i] : null;
}

function ls(thePath) {
  let result = {};

  let stats = fs.statSync(thePath);

  // ls file permissions. Convert into an easier format to use.
  if(!stats) {
    return console.error("Could not stat", thePath, ":", err);
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

  result = {
    perms: `${prefixChar}${permsResult}`,
    numLinks: stats.nlink,
    fileSize: stats.size,
    mtime: lsTime(stats.mtimeMs),
    basename: path.parse(path.basename(thePath)).name,
  };
  return result;
}

// Problem: the site is dynamically generated... so the files we're trying
// to ls don't exist.

function lsDynamicList(theDir, ext, ...files) {
  let fileStats = [];
  files.forEach((element) => {
    fileStats.push(ls(path.join(theDir, element + ext)));
  });
  console.log(fileStats);
  return fileStats;
}

function lsList(theDir, ...files) {
  let fileStats = [];
  files.forEach((element) => {
    fileStats.push(ls(path.join(theDir, element)));
  });
  return fileStats;
}

function lsDir(thePath) {
  let fileStats = [];
  fs.readdir(thePath, (err, files) => {
    if(err) {
      return console.log('Cannot scane directory ', thePath, ": ", err);
    }

    files.forEach((file) => {
      fileStats.push(ls(file));
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
  if(fs.exists(post)) {
    res.status(200).render('writing', { text : fs.readFile(post) });
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
  let test = lsDynamicList('public', '.html', 'main', 'software', 'sneed');
  console.log(test);
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
