const { createProxyMiddleware } = require('http-proxy-middleware');

module.exports = function(app) {
    app.use(
        '/api',
        createProxyMiddleware({
            target: 'http://localhost:8081',
            changeOrigin: true,
            pathRewrite: {
                '^/api': '/api'
            }
        })
    );

    app.use(
        '/auth',
        createProxyMiddleware({
            target: 'http://localhost:8080',
            changeOrigin: true,
            pathRewrite: {
                '^/auth': ''
            }
        })
    );
};