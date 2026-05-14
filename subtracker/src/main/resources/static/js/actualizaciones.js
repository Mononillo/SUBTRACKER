var eventSource = new EventSource('/api/suscripciones/stream');

eventSource.onmessage = function(event) {
    if (event.data === 'actualizado') {
        location.reload();
    }
};