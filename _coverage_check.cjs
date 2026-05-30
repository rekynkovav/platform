const fs = require('fs');
const path = require('path');

function walk(dir) {
  let results = [];
  try {
    fs.readdirSync(dir).forEach(f => {
      const p = path.join(dir, f);
      try {
        const s = fs.statSync(p);
        if (s.isDirectory()) results = results.concat(walk(p));
        else if (/\.java$/.test(p)) results.push(p);
      } catch(e) {}
    });
  } catch(e) {}
  return results;
}

const srcFiles = walk('backend/src/main/java/com/platform');
const testFiles = walk('backend/src/test/java/com/platform');

console.log('========================================');
console.log('   ПОКРЫТИЕ ТЕСТАМИ — ПОЛНЫЙ АНАЛИЗ');
console.log('========================================\n');
console.log(`Исходных файлов (src): ${srcFiles.length}`);
console.log(`Тестовых файлов (test): ${testFiles.length}\n`);

// Группировка исходников по пакетам
const packages = {};
srcFiles.forEach(f => {
  const rel = f.replace(/\\/g, '/').replace('backend/src/main/java/com/platform/', '');
  const pkg = rel.split('/')[0];
  if (!packages[pkg]) packages[pkg] = [];
  packages[pkg].push(rel);
});

console.log('=== ИСХОДНИКИ ПО ПАКЕТАМ ===');
Object.keys(packages).sort().forEach(pkg => {
  console.log(`  ${pkg}: ${packages[pkg].length} файлов`);
});

// Список тестов
const testNames = testFiles.map(f => path.basename(f, '.java'));
console.log('\n=== СПИСОК ТЕСТОВЫХ ФАЙЛОВ ===');
testFiles.forEach(f => {
  const rel = f.replace(/\\/g, '/').replace('backend/src/test/java/com/platform/', '');
  // Проверяем содержимое на пустые/заблокированные тесты
  const content = fs.readFileSync(f, 'utf8');
  const isDisabled = content.includes('@Disabled');
  const hasTodo = (content.match(/TODO/gi) || []).length;
  const hasAssert = /assert|verify|expect/.test(content);
  const methodCount = (content.match(/@Test/g) || []).length;
  
  let status = '✅';
  if (isDisabled) status = '🚫 DISABLED';
  else if (!hasAssert && methodCount > 0) status = '⚠️ NO ASSERTS';
  else if (methodCount === 0) status = '❌ EMPTY';
  
  console.log(`  ${status} ${rel} (${methodCount} tests${hasTodo ? ', ' + hasTodo + ' TODO' : ''})`);
});

// Анализ покрытия для controller и service
console.log('\n=== ПОКРЫТИЕ КОНТРОЛЛЕРОВ ===');
let controllerTotal = 0, controllerCovered = 0;
(packages['controller'] || []).forEach(file => {
  controllerTotal++;
  const name = path.basename(file, '.java');
  const hasTest = testNames.some(t => t === name + 'Test' || t === name + 'Tests' || t === name + 'IntegrationTest');
  if (hasTest) controllerCovered++;
  console.log(`  ${hasTest ? '✅' : '❌'} ${file}`);
});
console.log(`  Итого: ${controllerCovered}/${controllerTotal} контроллеров имеют тесты (${controllerTotal > 0 ? Math.round(controllerCovered/controllerTotal*100) : 0}%)`);

console.log('\n=== ПОКРЫТИЕ СЕРВИСОВ ===');
let serviceTotal = 0, serviceCovered = 0;
(packages['service'] || []).forEach(file => {
  serviceTotal++;
  const name = path.basename(file, '.java');
  const hasTest = testNames.some(t => t === name + 'Test' || t === name + 'Tests');
  if (hasTest) serviceCovered++;
  console.log(`  ${hasTest ? '✅' : '❌'} ${file}`);
});
console.log(`  Итого: ${serviceCovered}/${serviceTotal} сервисов имеют тесты (${serviceTotal > 0 ? Math.round(serviceCovered/serviceTotal*100) : 0}%)`);

console.log('\n=== ПОКРЫТИЕ SECURITY ===');
let secTotal = 0, secCovered = 0;
(packages['security'] || []).forEach(file => {
  secTotal++;
  const name = path.basename(file, '.java');
  const hasTest = testNames.some(t => t === name + 'Test' || t === name + 'Tests');
  if (hasTest) secCovered++;
  console.log(`  ${hasTest ? '✅' : '❌'} ${file}`);
});
(packages['filter'] || []).forEach(file => {
  secTotal++;
  const name = path.basename(file, '.java');
  const hasTest = testNames.some(t => t === name + 'Test' || t === name + 'Tests');
  if (hasTest) secCovered++;
  console.log(`  ${hasTest ? '✅' : '❌'} ${file}`);
});
console.log(`  Итого: ${secCovered}/${secTotal} security-компонентов имеют тесты (${secTotal > 0 ? Math.round(secCovered/secTotal*100) : 0}%)`);

console.log('\n=== ПОКРЫТИЕ WEBSOCKET ===');
let wsTotal = 0, wsCovered = 0;
(packages['websocket'] || []).forEach(file => {
  wsTotal++;
  const name = path.basename(file, '.java');
  const hasTest = testNames.some(t => t === name + 'Test' || t === name + 'Tests');
  if (hasTest) wsCovered++;
  console.log(`  ${hasTest ? '✅' : '❌'} ${file}`);
});
console.log(`  Итого: ${wsCovered}/${wsTotal} websocket-компонентов имеют тесты (${wsTotal > 0 ? Math.round(wsCovered/wsTotal*100) : 0}%)`);

console.log('\n=== ПОКРЫТИЕ UTIL/VALIDATION ===');
let utilTotal = 0, utilCovered = 0;
[...(packages['util'] || []), ...(packages['validation'] || [])].forEach(file => {
  utilTotal++;
  const name = path.basename(file, '.java');
  const hasTest = testNames.some(t => t === name + 'Test' || t === name + 'Tests');
  if (hasTest) utilCovered++;
  console.log(`  ${hasTest ? '✅' : '❌'} ${file}`);
});
console.log(`  Итого: ${utilCovered}/${utilTotal} утилит имеют тесты (${utilTotal > 0 ? Math.round(utilCovered/utilTotal*100) : 0}%)`);

// Общий итог
const totalCritical = controllerTotal + serviceTotal + secTotal + wsTotal;
const coveredCritical = controllerCovered + serviceCovered + secCovered + wsCovered;
console.log('\n========================================');
console.log('   ОБЩИЙ ИТОГ');
console.log('========================================');
console.log(`Всего исходных файлов: ${srcFiles.length}`);
console.log(`Всего тестовых файлов: ${testFiles.length}`);
console.log(`Покрытие критических компонентов: ${coveredCritical}/${totalCritical} (${Math.round(coveredCritical/totalCritical*100)}%)`);
console.log(`  Контроллеры: ${controllerCovered}/${controllerTotal} (${Math.round(controllerCovered/controllerTotal*100)}%)`);
console.log(`  Сервисы: ${serviceCovered}/${serviceTotal} (${Math.round(serviceCovered/serviceTotal*100)}%)`);
console.log(`  Security: ${secCovered}/${secTotal} (${secTotal > 0 ? Math.round(secCovered/secTotal*100) : 0}%)`);
console.log(`  WebSocket: ${wsCovered}/${wsTotal} (${wsTotal > 0 ? Math.round(wsCovered/wsTotal*100) : 0}%)`);
console.log(`  Util/Validation: ${utilCovered}/${utilTotal} (${utilTotal > 0 ? Math.round(utilCovered/utilTotal*100) : 0}%)`);
console.log('========================================\n');
