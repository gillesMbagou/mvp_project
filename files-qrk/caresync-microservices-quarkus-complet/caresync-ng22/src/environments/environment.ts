export const environment = {
  production: false,
  apiUrl: '/api/v1',
  keycloak: {
    url:      'http://localhost:8180',
    realm:    'caresync',
    clientId: 'caresync-frontend',
  },
  sseUrl: '/api/stream',
};
