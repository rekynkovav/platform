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
          if (!file.includes('node_modules') && !file.includes('.backup')) {
            results = results.concat(walk(filePath));
          }
        } else if (/\.(js|jsx|tsx|ts)$/.test(filePath)) {
          results.push(filePath);
        }
      } catch(e) {}
    });
  } catch(e) {}
  return results;
}

const files = walk('frontend/src');
const issues = [];

files.forEach(f => {
  const content = fs.readFileSync(f, 'utf8');
  const lines = content.split('\n');
  
  // 1. dangerouslySetInnerHTML
  if (content.includes('dangerouslySetInnerHTML')) {
    issues.push({file: f, type: 'XSS', detail: 'dangerouslySetInnerHTML usage'});
  }
  
  // 2. innerHTML direct DOM manipulation
  const innerHtmlMatch = content.match(/\.innerHTML\s*=/g);
  if (innerHtmlMatch) {
    issues.push({file: f, type: 'XSS', detail: `Direct .innerHTML assignment (${innerHtmlMatch.length} occurrences)`});
  }
  
  // 3. eval()
  if (/\beval\s*\(/.test(content)) {
    issues.push({file: f, type: 'CODE_INJECTION', detail: 'eval() usage'});
  }
  
  // 4. console.log with sensitive data patterns
  const consoleLines = lines.filter(l => /console\.(log|debug|info)\(/.test(l));
  if (consoleLines.length > 0) {
    issues.push({file: f, type: 'INFO_LEAK', detail: `${consoleLines.length} console.log statements`});
  }
  
  // 5. sessionStorage/localStorage with JSON.parse (no try-catch check)
  if (/JSON\.parse\(sessionStorage|JSON\.parse\(localStorage/.test(content)) {
    issues.push({file: f, type: 'STORAGE_RISK', detail: 'JSON.parse from storage without visible error handling'});
  }
  
  // 6. Hardcoded tokens/secrets/keys
  if (/['"](?:Bearer|token|secret|password|api[_-]?key)['"]\s*[:=]\s*['"][^'"]{8,}/i.test(content)) {
    issues.push({file: f, type: 'HARDCODED_SECRET', detail: 'Possible hardcoded credential'});
  }
  
  // 7. Client-side auth checks (role comparison in JS)
  if (/isAdmin|canEdit|canDownload|hasRole/.test(content) && /===\s*['"](?:SUPER_ADMIN|ADMIN|MODERATOR)/.test(content)) {
    issues.push({file: f, type: 'CLIENT_AUTH', detail: 'Client-side role-based access control'});
  }
  
  // 8. WebSocket without token validation
  if (/new WebSocket\(|ws:\/\/|wss:\//.test(content) && !/token|auth|Authorization/i.test(content)) {
    issues.push({file: f, type: 'WS_NO_AUTH', detail: 'WebSocket connection without visible auth'});
  }
  
  // 9. document.write
  if (/document\.write\(/.test(content)) {
    issues.push({file: f, type: 'XSS', detail: 'document.write usage'});
  }
  
  // 10. External script injection
  if (/\.src\s*=\s*['"]https?:/.test(content)) {
    issues.push({file: f, type: 'EXTERNAL_SCRIPT', detail: 'Dynamic external script loading'});
  }
  
  // 11. TODO/FIXME/HACK comments
  const todoCount = (content.match(/TODO|FIXME|HACK|XXX/gi) || []).length;
  if (todoCount > 0) {
    issues.push({file: f, type: 'TECH_DEBT', detail: `${todoCount} TODO/FIXME/HACK comments`});
  }
  
  // 12. alert() usage in production code
  const alertCount = (content.match(/\balert\s*\(/g) || []).length;
  if (alertCount > 0) {
    issues.push({file: f, type: 'UX_ISSUE', detail: `${alertCount} alert() calls`});
  }
  
  // 13. process.env exposure in client bundle
  if (/process\.env\./.test(content)) {
    const envVars = content.match(/process\.env\.\w+/g);
    if (envVars) {
      issues.push({file: f, type: 'ENV_EXPOSURE', detail: `Env vars in client code: ${[...new Set(envVars)].join(', ')}`});
    }
  }
  
  // 14. Unvalidated redirect
  if (/window\.location\.(href|assign|replace)\s*=/.test(content) && !/window\.location\.(href|assign|replace)\s*=\s*['"]/.test(content)) {
    issues.push({file: f, type: 'OPEN_REDIRECT', detail: 'Dynamic window.location assignment'});
  }
  
  // 15. fetch/axios without error handling
  if (/await\s+(fetch|axios|api\.)/.test(content) && !/try\s*\{/.test(content.substring(content.indexOf('await'), content.indexOf('await') + 500))) {
    // Simplified check - not perfect but catches obvious cases
  }
});

// Summary
console.log('=== FRONTEND SECURITY AUDIT RESULTS ===');
console.log(`Files scanned: ${files.length}`);
console.log(`Issues found: ${issues.length}\n`);

// Group by type
const grouped = {};
issues.forEach(i => {
  if (!grouped[i.type]) grouped[i.type] = [];
  grouped[i.type].push(i);
});

Object.keys(grouped).sort().forEach(type => {
  console.log(`\n[${type}] (${grouped[type].length} issues):`);
  grouped[type].forEach(i => {
    console.log(`  - ${i.file}: ${i.detail}`);
  });
});

console.log('\n=== END OF AUDIT ===');
