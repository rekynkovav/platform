const fs = require('fs');
const path = require('path');

function walk(dir) {
  let results = [];
  try {
    const list = fs.readdirSync(dir);
    list.forEach(file => {
      const filePath = path.join(dir, file);
      try {
        const stat = fs.statSync(filePath);
        if (stat && stat.isDirectory()) {
          results = results.concat(walk(filePath));
        } else if (/\.(js|jsx|tsx|ts)$/.test(filePath)) {
          results.push(filePath);
        }
      } catch(e) {}
    });
  } catch(e) {}
  return results;
}

const files = walk('frontend/src');
let foundXss = false;
files.forEach(f => {
  const content = fs.readFileSync(f, 'utf8');
  if (content.includes('dangerouslySetInnerHTML')) {
    console.log('FOUND dangerouslySetInnerHTML in: ' + f);
    foundXss = true;
  }
});
if (!foundXss) console.log('NOT FOUND: dangerouslySetInnerHTML in frontend/src');

// Check for GlobalExceptionHandler
const backendFiles = walk('backend/src');
let foundHandler = false;
backendFiles.forEach(f => {
  if (f.includes('GlobalExceptionHandler')) {
    console.log('FOUND GlobalExceptionHandler: ' + f);
    foundHandler = true;
  }
});
if (!foundHandler) console.log('NOT FOUND: GlobalExceptionHandler');

// Check for SecureMediaService
backendFiles.forEach(f => {
  if (f.includes('SecureMediaService')) {
    console.log('FOUND SecureMediaService: ' + f);
  }
});

// Check for RateLimiting annotation usage
let rateLimitCount = 0;
backendFiles.forEach(f => {
  const content = fs.readFileSync(f, 'utf8');
  if (content.includes('@RateLimiting')) rateLimitCount++;
  if (content.includes('// @RateLimiting')) console.log('COMMENTED RateLimiting in: ' + f);
});
console.log('Active @RateLimiting annotations found: ' + rateLimitCount);
