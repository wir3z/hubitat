# Securing the OwnTracks app

How to lock down the OwnTracks Hubitat app's web links and location reporting. It needs version **1.9.11 or later**. Each step builds on the one before, so stop at the one that fits your setup. Step 1 needs nothing but the app.

## Why it matters

Hubitat gives the app **one access token**. The mobile devices use it to report locations, and every web link carries it too. With the default settings, anyone holding a single dashboard link can:

- see every member's location and history
- add, move or delete regions, and send member commands
- post fake location reports, through the Hubitat cloud, with no further login

A link doesn't have to be stolen to leak. Forwarding it, a browser history or a screenshot is enough.

## At a glance

| Setting | Where | What it protects |
|---|---|---|
| **Require a view key** | Configure Web Access | A per-person key in each link, checked on top of the token. Revoke one person without touching the phones |
| **Remote access** | Configure Web Access | The single way the web pages are reached from outside the home: the Hubitat cloud, a reverse proxy, or neither. The others are refused |
| **Mobile URL** | Mobile App Installation Instructions | Which URL the phones are told to report to: the cloud, local or VPN, or a reverse proxy |
| **Block all Hubitat cloud requests** | Top of Installation and Configuration | Refuses every request to this app through cloud.hubitat.com, including location reports |

## Step 1: Require view keys (all setups)

In **Configure Web Access**, turn on **Require a view key**. From then on, every map, member page, map data request and member command must carry a key the app issued (`&k=…`). Links without one, or with a revoked one, are refused.

1. Add a key for each person or device you share links with. Set *Full access for the next key added* first if needed, then type the name in **Add a view key** and press Enter.
   - **View** keys open the maps and member pages.
   - **Full** keys can also edit regions, use the geocoding quota and send member commands. Keep those for yourself.
2. In **Dashboard Web Links** and **Member Command API Links**, use **Show links for view key** to show each person's links, then send them those.
3. Press **Done** after adding, revoking or regenerating keys, so the dashboard tiles are rebuilt.

To cut someone off, revoke their key. Nobody else is affected, and the phones keep working.

The app manages two keys of its own: *Hub app pages*, for the maps inside the app, and *Dashboard tiles*. Regenerate *Dashboard tiles* if a dashboard was shared more widely than you meant.

View keys protect the web pages only. Location reports still need only the token, because the phones can't carry a key. Steps 4 and 5 cover the phones.

## Step 2: Choose a single way in (Remote access)

In **Configure Web Access**, set **Remote access** to how the web pages should be reached from outside the home network. Requests that arrive any other way are refused. Requests made directly on the home network always work.

| Remote access | Through cloud.hubitat.com | Through a reverse proxy | On the home network |
|---|---|---|---|
| Hubitat cloud (the default) | Allowed | Refused | Allowed |
| External URL (reverse proxy) | Refused | Allowed | Allowed |
| None - local network only | Refused | Refused | Allowed |

The links and the dashboard tiles follow this choice, and the local links are always listed too. If you only use the dashboards at home, or over a VPN, choose **None**. Then the web pages aren't reachable from the internet at all.

A reverse proxy is recognised by the `X-Forwarded-For` or `X-Forwarded-Host` header it adds, which Apache and nginx send by default. A proxy configured not to send them looks like a request on the home network, and so does a port forward straight to the hub. See [Don't port-forward the hub](#dont-port-forward-the-hub).

## Step 3: Serve the web pages through your own reverse proxy (optional)

This keeps the token out of the links entirely and puts a login in front of the pages. You'll need a web server you control with HTTPS, such as Apache or nginx, that can reach the hub.

**On the proxy:**

1. **Forward only the pages that need to be reachable.** For the maps, those are `googlemap`, `apidata`, `membermap/<member>` and `memberpresence/<member>`, all under `/apps/api/<app id>/` on the hub. Refuse everything else. Leaving out the region configuration map, member commands, Recorder pages and the webhook keeps the admin and command pages off the internet.
2. **Require a login** before forwarding anything.
3. **Add the token yourself,** replacing the browser's login header:
   - `Authorization: Bearer <access token>`
   - `X-OwnTracks-Token-Injected: 1`. The hub accepts the Bearer token but doesn't show that header to the app, so this second header is how the app knows to leave the token out of the page.

   Keep the token in a file only the proxy can read, not in a public config repository.
4. **Keep query strings out of your access logs.** With view keys on, every link carries one.
5. **Rate-limit or ban failed logins,** with fail2ban or similar. Most web servers have no lockout of their own.

**In the app**, in **Configure Web Access**:

1. Set **Remote access** to *External URL (reverse proxy)*.
2. Enter the base URL that your proxy forwards to the hub's local app URL shown in the setting, for example `https://example.com/owntracks-web`.
3. Turn on **Omit the access token from the links**. The app then shows *Access token for the reverse proxy*, the value for step 3 above.

**Keep *Require a view key* on.** Because the proxy adds the token for anyone who passes its login, the login would otherwise be the only secret an outsider needs. The view key comes from the link and is checked at the hub, so it's a second, independent secret. A shared household login on the proxy plus per-person view keys in the app works well. You manage people in the app, and the proxy's password file never needs editing.

**Google Maps:** restrict the Maps JavaScript API key by HTTP referrer to your proxy's site, for example `https://example.com/*`. Restrict by site, not by path: browsers usually send only the origin as the referrer, so a `…/owntracks-web/*` restriction rejects your own pages with `RefererNotAllowedMapError`.

### Example: Apache

This forwards the four pages at `https://example.com/owntracks-web/`, behind a Basic login, with the token added from a root-only file. Replace `<app id>`, the hub address and the file paths with your own.

```apache
# /etc/apache2/owntracks-web-token.inc, mode 600, holds:
#   Define OWNTRACKS_HUB_TOKEN <access token>
Include /etc/apache2/owntracks-web-token.inc

<Location "/owntracks-web">
    Require all denied
</Location>

<LocationMatch "^/owntracks-web/(googlemap|apidata|membermap/[^/]+|memberpresence/[^/]+)$">
    AuthType Basic
    AuthName "owntracks-web"
    AuthBasicProvider file
    AuthUserFile /etc/apache2/owntracks-web.htpasswd
    <RequireAll>
        Require valid-user
        Require method GET POST
    </RequireAll>
    RequestHeader set Authorization "Bearer ${OWNTRACKS_HUB_TOKEN}"
    RequestHeader set X-OwnTracks-Token-Injected "1"
</LocationMatch>

ProxyPassMatch "^/owntracks-web/(googlemap|apidata|membermap/[^/]+|memberpresence/[^/]+)$" "https://<hub address>/apps/api/<app id>/$1" timeout=15
```

Put this in the HTTPS virtual host only, never on plain HTTP, because the login is sent with every request. The pages use relative URLs, so the proxy path can be anything you like.

Hubitat serves plain `http` on the home network by default, so `<hub address>` is usually `http://<hub IP>`. If you've enabled HTTPS on the hub, use `https://` instead, and add `SSLProxyEngine on` to the virtual host. Also configure `SSLProxyCACertificateFile` to verify the hub's certificate, rather than turning verification off. `mod_proxy`, `mod_proxy_http` and `mod_headers` must be enabled.

## Step 4: Move the phones off the cloud (optional)

The phones can report through your own reverse proxy instead of the Hubitat cloud:

1. Forward a path on your proxy, for example `https://example.com/owntracks`, to the hub's `/apps/api/<app id>/webhook`. Allow POST only, and require a login. OwnTracks sends the *Username* and *Password* from its Identification settings as a Basic login.
2. In **Mobile App Installation Instructions**, set **Mobile URL** to *External URL (reverse proxy)*, and enter that URL.
   - Leave **Omit the access token** off, unless your proxy adds the token for the phones too.
3. Update each phone with the URL and password shown. The app can't push a new URL to a phone.

Give the phones their own path and login, separate from the web pages, so a phone's password can't open the maps.

### Example: Apache for the phones

This accepts location reports at `https://example.com/owntracks`, behind a Basic login per phone. Unlike the web pages, the phone keeps `?access_token=` in its URL, so the proxy doesn't need to know the token.

```apache
<Location "/owntracks">
    AuthType Basic
    AuthName "owntracks"
    AuthBasicProvider file
    AuthUserFile /etc/apache2/owntracks.htpasswd
    <RequireAll>
        Require valid-user
        # OwnTracks only ever POSTs location reports
        Require method POST
    </RequireAll>
    # the hub has no use for the phone's password
    RequestHeader unset Authorization
    ProxyPass "http://<hub IP>/apps/api/<app id>/webhook" timeout=15
</Location>
```

- Create one entry per phone with `htpasswd -B /etc/apache2/owntracks.htpasswd <Username>`, using `-c` for the first only. The name must match the phone's OwnTracks *Username* exactly, including case, because the app also uses it to tell members apart.
- Put it in the HTTPS virtual host only, because the password is sent with every report.
- The same `timeout=15` bounds how long a hung hub can tie up the proxy.
- Rate-limit or ban failed logins here too.

Mobile URL is independent of Remote access. For example, the phones can keep reporting through the Hubitat cloud while Remote access is *None*, which keeps the web pages off the internet.

## Step 5: Block the Hubitat cloud entirely

Once **every** phone reports through a local, VPN or reverse proxy URL, turn on **Block all Hubitat cloud requests** at the top of **Installation and Configuration**. Every request to this app through cloud.hubitat.com is then refused, including location reports, so a leaked cloud URL is useless.

Phones still set to the cloud URL stop updating, with no message on the phone. Check that each member is reporting before you turn it on. The hub log shows *Rejected a location report through the Hubitat cloud* for any that aren't.

This only affects this app. Hubitat's own remote access, other apps and dashboards are untouched.

## Don't port-forward the hub

Forwarding a port on your router straight to the hub, instead of using a reverse proxy, looks simpler but is the riskiest option:

- **It exposes the whole hub, not just this app.** That includes the hub's admin pages, every other app's endpoints (such as Maker API) and the file manager. Many hubs have no login on the local admin pages.
- **Nothing is encrypted.** The hub serves plain `http` on the home network, so the token and every location would cross the internet in clear text. Enabling HTTPS on the hub only gives a self-signed certificate, which the mobile apps reject.
- **There is no login or rate limiting** in front of the token.
- **Remote access can't see it.** A port-forwarded request carries no forwarding headers, so the app treats it as a request on the home network, which is always allowed. Remote access *None* will not refuse it.

To reach the hub from outside, use the Hubitat cloud, a VPN to your home network, or a reverse proxy with HTTPS and a login.

## Recommended setups

| You want | Remote access | Mobile URL | Block all Hubitat cloud requests | View keys |
|---|---|---|---|---|
| The simplest setup, with the cloud for everything | Hubitat cloud | Hubitat cloud | Off | **On** |
| Phones on the cloud, but no dashboards or controls on the internet | None | Hubitat cloud | Off | On |
| Everything over a VPN | None | Local network or VPN | On | On |
| Your own reverse proxy for pages and phones | External URL | External URL | On | **On**, the second factor behind the proxy |

## Checking it works

Replace `$TOKEN`, `$HUB` (for example `https://192.168.1.10/apps/api/<app id>`) and `$CLOUD` (the cloud link up to and including the app id) with your own values.

```bash
# Without a key: refused when view keys are required
curl -s "$HUB/memberpresence/<member>?access_token=$TOKEN" | head -c 80

# Through the cloud, with Remote access not set to Hubitat cloud: refused
curl -s -X POST -H 'Content-Type: application/json' -d '{"action":"members","payload":{}}' \
  "$CLOUD/apidata?access_token=$TOKEN" -w ' %{http_code}\n'     # expect [] 403

# Cloud location report with Block all Hubitat cloud requests on: refused
curl -s -X POST "$CLOUD/webhook?access_token=$TOKEN" -w ' %{http_code}\n'    # expect 403
```

Through a proxy that adds the token, view the page source of the family map. It should contain no `access_token`.

## Limits worth knowing

- **The token is shared with the phones.** It's in every phone's settings, so it's only as secret as the phones. Rotating it means updating every phone, and your proxy if it adds the token.
- **View keys are bearer secrets.** Anyone with a person's link, and the proxy login if there is one, gets that person's access until you revoke the key.
- **A proxy that adds the token holds it.** Protect that server like anything else that holds a credential.
- **Remote access only sees the cloud and reverse proxies.** It recognises a proxy by its forwarding headers. A port forward to the hub, or a proxy that strips those headers, looks like the home network and is always allowed.
- **Hubitat's cloud relay can't be turned off for one app.** Requests for this app will still reach the hub through cloud.hubitat.com. These settings make the app refuse them, which is as close to closed as the app can get.
