// Le dev-server d'Angular 21 (@angular/build:dev-server) délègue le proxy à
// Vite, qui utilise http-proxy en interne. Le pass de sortie par défaut
// d'http-proxy (OUTGOING_PASSES.removeChunked) retire l'en-tête
// Transfer-Encoding de la réponse proxifiée, et Vite semble ensuite
// bufferiser indéfiniment tout flux chunked sans Content-Length avant de
// (re)générer ses propres en-têtes — un flux SSE (/api/stream/*, jamais
// terminé) reste alors bloqué sans qu'aucun octet n'atteigne jamais le
// client, alors que la même requête vers le gateway (:8090) en direct
// répond instantanément. Un appel REST classique n'est pas affecté (il a
// un Content-Length connu dès le départ).
//
// selfHandleResponse: true désactive les passes de sortie par défaut
// d'http-proxy (dont removeChunked) et laisse le hook proxyRes ci-dessous
// écrire les en-têtes en un seul writeHead() synchrone puis relayer le
// corps au fil de l'eau via un pipe manuel — ce qui contourne le problème
// pour /api/stream/* tout en gardant le comportement normal pour le reste.
export default {
  '/api': {
    target: 'http://localhost:8090',
    secure: false,
    changeOrigin: true,
    logLevel: 'info',
    selfHandleResponse: true,
    configure: (proxy) => {
      proxy.on('proxyRes', (proxyRes, req, res) => {
        res.writeHead(proxyRes.statusCode, proxyRes.headers);
        // Node ne transmet les en-têtes qu'au premier write() sur la réponse —
        // pour un flux SSE qui peut rester silencieux un long moment (aucun
        // événement Kafka en attente), le client resterait bloqué sans même
        // recevoir le 200/Content-Type tant qu'aucune donnée n'arrive.
        // flushHeaders() force leur envoi immédiat, indépendamment du corps.
        res.flushHeaders?.();
        proxyRes.pipe(res);
      });
      proxy.on('error', (err, req, res) => {
        if (!res.headersSent) res.writeHead(502);
        res.end(`Proxy error: ${err.message}`);
      });
    },
  },
};
