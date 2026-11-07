# Consentimiento y privacidad — PIC k150 Programing

- **Paquete:** `com.diamon.pic`
- **Política de privacidad:** https://politicasdeprivacidaspickprograming.blogspot.com/2024/12/pic-k150-programming.html?m=1
- **SDK de consentimiento:** Google User Messaging Platform (UMP) 4.0.0
- **Última actualización:** 2026-11-07

Este documento reúne los textos de consentimiento que deben configurarse en **AdMob → Privacidad y mensajes** y el comportamiento que implementa la app.

## Comportamiento implementado en la app

1. Al arrancar se llama a `requestConsentInfoUpdate` (UMP).
2. Si corresponde, se muestra el formulario de consentimiento.
3. `MobileAds.initialize` y cualquier petición de anuncio esperan a que el consentimiento esté resuelto.
4. Si el usuario no consiente, no se carga ningún anuncio.
5. «Opciones de privacidad» solo aparece cuando UMP indica que es obligatorio.
6. Al cerrar ese formulario se reaplica el estado: si acepta, se cargan los anuncios pendientes; si rechaza, se retiran.

## Mensaje de Normativas europeas

Configuración recomendada en AdMob:

- **Segmentación:** Países sujetos al RGPD (EEE, Reino Unido y Suiza).
- **No consentir:** Activado.
- **Idioma predeterminado:** Español; añade los demás idiomas de esta lista.

### Español

- **Título:** Tu privacidad
- **Texto:** Usamos tus datos para mostrar anuncios personalizados, medir su rendimiento y mejorar la aplicación. Puedes aceptar, rechazar o cambiar tus opciones cuando quieras desde el menú «Opciones de privacidad».
- **Botón aceptar:** Consentir
- **Botón rechazar:** No consentir
- **Enlace de opciones:** Opciones de privacidad
- **Enlace de política:** Política de privacidad

### English

- **Título:** Your privacy
- **Texto:** We use your data to show personalised ads, measure their performance and improve the app. You can accept, decline or change your choices at any time from the "Privacy options" menu.
- **Botón aceptar:** Consent
- **Botón rechazar:** Do not consent
- **Enlace de opciones:** Privacy options
- **Enlace de política:** Privacy policy

### Português

- **Título:** Sua privacidade
- **Texto:** Usamos seus dados para exibir anúncios personalizados, medir o desempenho e melhorar o aplicativo. Você pode aceitar, recusar ou alterar suas escolhas a qualquer momento no menu «Opções de privacidade».
- **Botón aceptar:** Consentir
- **Botón rechazar:** Não consentir
- **Enlace de opciones:** Opções de privacidade
- **Enlace de política:** Política de privacidade

### हिन्दी

- **Título:** आपकी गोपनीयता
- **Texto:** हम व्यक्तिगत विज्ञापन दिखाने, उनका प्रदर्शन मापने और ऐप को बेहतर बनाने के लिए आपके डेटा का उपयोग करते हैं। आप «गोपनीयता विकल्प» मेनू से कभी भी सहमति दे सकते हैं, मना कर सकते हैं या अपनी पसंद बदल सकते हैं।
- **Botón aceptar:** सहमति दें
- **Botón rechazar:** सहमति न दें
- **Enlace de opciones:** गोपनीयता विकल्प
- **Enlace de política:** गोपनीयता नीति

### Bahasa Indonesia

- **Título:** Privasi Anda
- **Texto:** Kami menggunakan data Anda untuk menampilkan iklan yang dipersonalisasi, mengukur performanya, dan meningkatkan aplikasi. Anda dapat menyetujui, menolak, atau mengubah pilihan kapan saja melalui menu «Opsi privasi».
- **Botón aceptar:** Setuju
- **Botón rechazar:** Tidak setuju
- **Enlace de opciones:** Opsi privasi
- **Enlace de política:** Kebijakan privasi

## Mensaje de Normativas estatales de EE. UU.

Se muestra únicamente desde el punto de entrada «Opciones de privacidad» en los estados regulados.

### Español

- **Título:** Tus opciones de privacidad en EE. UU.
- **Texto:** Puedes solicitar que no se venda ni se comparta tu información personal con fines de publicidad personalizada.
- **Enlace:** No vender ni compartir mi información personal
- **Botón excluirse:** Excluirme
- **Botón descartar:** Descartar

### English

- **Título:** Your US privacy choices
- **Texto:** You can request that your personal information is not sold or shared for personalised advertising.
- **Enlace:** Do Not Sell or Share My Personal Information
- **Botón excluirse:** Opt out
- **Botón descartar:** Dismiss

### Português

- **Título:** Suas opções de privacidade nos EUA
- **Texto:** Você pode solicitar que suas informações pessoais não sejam vendidas nem compartilhadas para publicidade personalizada.
- **Enlace:** Não vender nem compartilhar minhas informações pessoais
- **Botón excluirse:** Excluir-me
- **Botón descartar:** Descartar

### हिन्दी

- **Título:** अमेरिका में आपकी गोपनीयता के विकल्प
- **Texto:** आप अनुरोध कर सकते हैं कि व्यक्तिगत विज्ञापन के लिए आपकी व्यक्तिगत जानकारी न बेची जाए और न साझा की जाए।
- **Enlace:** मेरी व्यक्तिगत जानकारी न बेचें और न साझा करें
- **Botón excluirse:** बाहर निकलें
- **Botón descartar:** बंद करें

### Bahasa Indonesia

- **Título:** Pilihan privasi Anda di AS
- **Texto:** Anda dapat meminta agar informasi pribadi Anda tidak dijual atau dibagikan untuk iklan yang dipersonalisasi.
- **Enlace:** Jangan jual atau bagikan informasi pribadi saya
- **Botón excluirse:** Tolak
- **Botón descartar:** Tutup

## Mensajes que NO aplican

- **IDFA:** exclusivo de iOS.
- **Corea:** AdMob no ofrece un mensaje específico para Android; solo aplican europeas y estatales de EE. UU.

## Notas

- La política de privacidad debe ser accesible públicamente y coincidir con lo que declara la ficha de Google Play.
- El host `todoandroid.42web.io` aplica un reto JavaScript antirrobots: los navegadores lo superan, pero conviene comprobar la URL con el validador de AdMob antes de publicar.
