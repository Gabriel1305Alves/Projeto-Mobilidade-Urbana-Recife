export function obterPosicao() {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error("Este aparelho não tem GPS."));
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => resolve({
        lat: pos.coords.latitude,
        lng: pos.coords.longitude,
      }),
      (err) => reject(new Error(
        err.code === 1
          ? "Permita o acesso à localização no navegador."
          : "Não deu para achar sua posição."
      )),
      { enableHighAccuracy: true, timeout: 12000, maximumAge: 60000 }
    );
  });
}

export async function nomeDoLocal(lat, lng) {
  try {
    const resposta = await fetch(
      `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${lat}&lon=${lng}`
    );
    const dados = await resposta.json();
    const endereco = dados.address || {};
    return endereco.road
      || endereco.suburb
      || endereco.neighbourhood
      || endereco.city
      || "Localização capturada";
  } catch {
    return "Localização capturada";
  }
}
