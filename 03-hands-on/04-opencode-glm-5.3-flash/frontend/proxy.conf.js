// Proxy do dev server do Angular: encaminha as chamadas relativas ao backend.
// O painel sempre chama "api/..." (relativo) — o Traefik encaminha /template/api
// ao backend no deploy do template; em ambientes sem esse roteamento (sandbox
// com hostnames separados, npm start local) quem resolve é este proxy.
const target = process.env.API_PROXY_TARGET || 'http://localhost:8080';

module.exports = {
  '/api': { target, secure: false, pathRewrite: { '^/api': '' } },
  '/template/api': { target, secure: false, pathRewrite: { '^/template/api': '' } },
};
