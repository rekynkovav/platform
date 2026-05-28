const fs = require('fs');
const path = require('path');

function walk(dir) {
  let results = [];
  const list = fs.readdirSync(dir);
  list.forEach(file => {
    const filePath = path.join(dir, file);
    const stat = fs.statSync(filePath);
    if (stat && stat.isDirectory()) {
      results = results.concat(walk(filePath));
    } else if (/\.(js|jsx|tsx|ts)$/.test(filePath)) {
      results.push(filePath);
    }
  });
  return results;
}

const files = walk('frontend/src');
let found = false;
files.forEach(f => {
  const content = fs.readFileSync(f, 'utf8');
  if (content.includes('dangerouslySetInnerHTML')) {
    console.log('FOUND dangerouslySetInnerHTML in: ' + f);
    found = true;
  }
});
if (!found) console.log('NOT FOUND: dangerouslySetInnerHTML');
