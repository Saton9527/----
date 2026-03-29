function t(e){const r=e.trim().toUpperCase().match(/^CF\s+(\d+)([A-Z]\d*)$/);return r?`https://codeforces.com/problemset/problem/${r[1]}/${r[2]}`:null}export{t as r};
