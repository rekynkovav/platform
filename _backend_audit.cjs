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
        } else if (/\.java$/.test(filePath)) {
          results.push(filePath);
        }
      } catch(e) {}
    });
  } catch(e) {}
  return results;
}

const files = walk('backend/src/main/java/com/platform');
const issues = [];

files.forEach(f => {
  const content = fs.readFileSync(f, 'utf8');
  const relPath = f.replace(/\\/g, '/');
  const lines = content.split('\n');

  // 1. SQL Injection - string concatenation in queries
  if (/\+\s*["'].*(?:SELECT|INSERT|UPDATE|DELETE|WHERE|FROM)/i.test(content) ||
      /String\.format\(.*(?:SELECT|INSERT|UPDATE|DELETE)/i.test(content)) {
    issues.push({file: relPath, type: 'SQL_INJECTION', detail: 'String concatenation/format in SQL query'});
  }

  // 2. Missing @Valid on controller methods
  const controllerMethods = content.match(/@\w+Mapping.*\n.*public.*\([^)]*@(?:RequestBody|ModelAttribute)[^)]*\)/gs);
  if (controllerMethods) {
    controllerMethods.forEach(m => {
      if (!m.includes('@Valid') && !m.includes('@Validated')) {
        issues.push({file: relPath, type: 'MISSING_VALIDATION', detail: '@RequestBody/@ModelAttribute without @Valid'});
      }
    });
  }

  // 3. Sensitive data in logs
  if (/log\.(info|debug|warn|error)\(.*(?:password|token|secret|creditCard|ssn)/i.test(content)) {
    issues.push({file: relPath, type: 'SENSITIVE_LOG', detail: 'Possible sensitive data in log statement'});
  }

  // 4. Hardcoded credentials/secrets
  if (/["'](?:password|secret|apiKey|api_key|access_key)["']\s*[=:]\s*["'][^"']{4,}["']/i.test(content) &&
      !/\$\{|@Value|System\.getenv|getProperty/.test(content)) {
    issues.push({file: relPath, type: 'HARDCODED_SECRET', detail: 'Possible hardcoded credential'});
  }

  // 5. Catch Exception without proper handling
  const catchBlocks = content.match(/catch\s*\(\s*Exception\s+\w+\s*\)/g);
  if (catchBlocks && !relPath.includes('GlobalExceptionHandler')) {
    issues.push({file: relPath, type: 'BROAD_CATCH', detail: `${catchBlocks.length} catch(Exception) blocks`});
  }

  // 6. TODO/FIXME/HACK
  const todoCount = (content.match(/TODO|FIXME|HACK|XXX/gi) || []).length;
  if (todoCount > 0) {
    issues.push({file: relPath, type: 'TECH_DEBT', detail: `${todoCount} TODO/FIXME/HACK comments`});
  }

  // 7. System.out/System.err instead of logger
  if (/System\.(out|err)\.(print|println)/.test(content)) {
    issues.push({file: relPath, type: 'BAD_LOGGING', detail: 'System.out/err instead of SLF4J logger'});
  }

  // 8. Thread.sleep in non-test code
  if (/Thread\.sleep\(/.test(content) && !relPath.includes('test')) {
    issues.push({file: relPath, type: 'BLOCKING_CALL', detail: 'Thread.sleep() in production code'});
  }

  // 9. @Transactional missing on service write methods
  if (relPath.includes('Service.java') && !relPath.includes('Test')) {
    const publicMethods = content.match(/public\s+(?!class)\w+\s+\w+\(/g);
    const hasTransactional = content.includes('@Transactional');
    if (publicMethods && publicMethods.length > 0 && !hasTransactional) {
      issues.push({file: relPath, type: 'MISSING_TRANSACTION', detail: 'Service class without @Transactional'});
    }
  }

  // 10. Path traversal risk - file operations with user input
  if (/new File\(|Paths\.get\(|Path\.of\(/.test(content) && 
      /getParameter|@RequestParam|@PathVariable|request\./.test(content)) {
    if (!/normalize|canonical|startsWith/.test(content)) {
      issues.push({file: relPath, type: 'PATH_TRAVERSAL', detail: 'File operation with user input without path validation'});
    }
  }

  // 11. Insecure random
  if (/new Random\(\)|Math\.random\(\)/.test(content) && 
      /token|session|key|salt|nonce|uuid/i.test(content)) {
    issues.push({file: relPath, type: 'INSECURE_RANDOM', detail: 'Non-secure random for security-sensitive operation'});
  }

  // 12. Missing CSRF protection check
  if (relPath.includes('SecurityConfig') && /ignoringRequestMatchers|permitAll/.test(content)) {
    const ignored = content.match(/ignoringRequestMatchers\([^)]+\)|permitAll\(\)/g);
    if (ignored) {
      issues.push({file: relPath, type: 'CSRF_BYPASS', detail: `CSRF/auth bypass: ${ignored.length} patterns`});
    }
  }

  // 13. Response entity returning full entity objects
  if (/ResponseEntity<\w+>.*return.*repository\./s.test(content)) {
    issues.push({file: relPath, type: 'DATA_EXPOSURE', detail: 'Direct repository entity in response (possible over-exposure)'});
  }

  // 14. Commented-out security annotations
  if (/\/\/\s*@(?:PreAuthorize|Secured|RolesAllowed|RateLimiting)/.test(content)) {
    issues.push({file: relPath, type: 'DISABLED_SECURITY', detail: 'Commented-out security annotation'});
  }

  // 15. e.printStackTrace()
  if (/\.printStackTrace\(\)/.test(content)) {
    issues.push({file: relPath, type: 'STACK_TRACE_LEAK', detail: 'e.printStackTrace() usage'});
  }
});

// Summary
console.log('=== BACKEND SECURITY AUDIT RESULTS ===');
console.log(`Files scanned: ${files.length}`);
console.log(`Issues found: ${issues.length}\n`);

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
