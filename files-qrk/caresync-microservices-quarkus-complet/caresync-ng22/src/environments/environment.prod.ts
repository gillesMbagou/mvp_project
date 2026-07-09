export const environment = {
  production: true,
  apiUrl: 'https://api.caresync.be/api/v1',
  keycloak: {
    url:      'https://auth.caresync.be',
    realm:    'caresync',
    clientId: 'caresync-frontend',
  },
  sseUrl: 'https://api.caresync.be/api/stream',
};
