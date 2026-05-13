/**
 * ログイン画面: 外側 #login-roach-stage を left/top のみで動かし、内側 #login-roach-scale で 0.1 倍表示。
 * 通常はゆっくり。カーソルがパネル寸前に来たときだけ、残り 1〜3 回まで高速で逃げる。
 */
(function () {
    'use strict';
    var stage = document.getElementById('login-roach-stage');
    var scaled = document.getElementById('login-roach-scale');
    if (!stage || !scaled) return;

    /* 通常 = 従来のおおよそ半分 */
    var NORMAL_MIN = 100;
    var NORMAL_MAX = 260;
    var NORMAL_JITTER = 1700;
    var NORMAL_BOUNCE_LO = 40;
    var NORMAL_BOUNCE_HI = 100;

    /* パニック = 従来クラスの速さ */
    var PANIC_MIN = 200;
    var PANIC_MAX = 520;
    var PANIC_JITTER = 3400;
    var PANIC_BOUNCE_LO = 80;
    var PANIC_BOUNCE_HI = 200;

    var TURN_CHANCE = 0.05;
    var PAUSE_CHANCE = 0.006;
    var NEAR_DIST = 70;
    var LEAVE_DIST = 100;
    var PANIC_MS_MIN = 420;
    var PANIC_MS_MAX = 780;

    var timerId = null;
    var lastTs = 0;
    var vx = 0;
    var vy = 0;
    var x = 0;
    var y = 0;
    var frozenUntil = 0;

    var panicChargesLeft = 0;
    var panicActiveUntil = 0;
    var panicArm = true;

    function rand(min, max) {
        return min + Math.random() * (max - min);
    }
    function clamp(n, lo, hi) {
        return Math.max(lo, Math.min(hi, n));
    }
    function nowTs() {
        return typeof performance !== 'undefined' && performance.now ? performance.now() : Date.now();
    }
    function visualSize() {
        var rect = scaled.getBoundingClientRect();
        return { w: rect.width, h: rect.height };
    }
    function setStagePos(px, py) {
        x = px;
        y = py;
        stage.style.left = Math.round(px) + 'px';
        stage.style.top = Math.round(py) + 'px';
    }
    function placeInBounds() {
        var sz = visualSize();
        var pad = 8;
        var maxX = Math.max(pad, window.innerWidth - sz.w - pad);
        var maxY = Math.max(pad, window.innerHeight - sz.h - pad);
        setStagePos(clamp(x, pad, maxX), clamp(y, pad, maxY));
    }
    function distPointToRect(px, py, r) {
        var cx = clamp(px, r.left, r.right);
        var cy = clamp(py, r.top, r.bottom);
        return Math.hypot(px - cx, py - cy);
    }
    function randomHeading(minS, maxS) {
        var ang = Math.random() * Math.PI * 2;
        var sp = rand(minS, maxS);
        vx = Math.cos(ang) * sp;
        vy = Math.sin(ang) * sp;
    }
    function pickLimits(ts) {
        if (ts < panicActiveUntil) {
            return {
                minS: PANIC_MIN,
                maxS: PANIC_MAX,
                jit: PANIC_JITTER,
                bLo: PANIC_BOUNCE_LO,
                bHi: PANIC_BOUNCE_HI
            };
        }
        return {
            minS: NORMAL_MIN,
            maxS: NORMAL_MAX,
            jit: NORMAL_JITTER,
            bLo: NORMAL_BOUNCE_LO,
            bHi: NORMAL_BOUNCE_HI
        };
    }
    function tick() {
        var ts = nowTs();
        if (!lastTs) lastTs = ts;
        var dt = Math.min(0.08, (ts - lastTs) / 1000);
        lastTs = ts;

        var L = pickLimits(ts);

        if (ts < frozenUntil) {
            return;
        }
        if (Math.random() < PAUSE_CHANCE) {
            frozenUntil = ts + rand(80, 360);
            return;
        }

        vx += (Math.random() - 0.5) * L.jit * dt;
        vy += (Math.random() - 0.5) * L.jit * dt;
        if (Math.random() < TURN_CHANCE) {
            if (Math.random() < 0.5) {
                var t = vx;
                vx = -vy;
                vy = t;
            } else {
                randomHeading(L.minS, L.maxS);
            }
        }

        var speed = Math.hypot(vx, vy);
        if (speed < 1e-6) {
            randomHeading(L.minS, L.maxS);
            speed = Math.hypot(vx, vy);
        }
        if (speed < L.minS && speed > 1e-6) {
            vx = (vx / speed) * L.minS;
            vy = (vy / speed) * L.minS;
        } else if (speed > L.maxS) {
            vx = (vx / speed) * L.maxS;
            vy = (vy / speed) * L.maxS;
        }

        x += vx * dt;
        y += vy * dt;

        var sz = visualSize();
        var pad = 6;
        var maxX = Math.max(pad, window.innerWidth - sz.w - pad);
        var maxY = Math.max(pad, window.innerHeight - sz.h - pad);
        if (x <= pad) {
            x = pad;
            vx = Math.abs(vx) + rand(L.bLo, L.bHi);
        } else if (x >= maxX) {
            x = maxX;
            vx = -(Math.abs(vx) + rand(L.bLo, L.bHi));
        }
        if (y <= pad) {
            y = pad;
            vy = Math.abs(vy) + rand(L.bLo, L.bHi);
        } else if (y >= maxY) {
            y = maxY;
            vy = -(Math.abs(vy) + rand(L.bLo, L.bHi));
        }

        setStagePos(x, y);
    }

    function onPointerNear(ev) {
        if (!stage.classList.contains('login-fleeing')) return;
        if (panicChargesLeft <= 0) return;

        var px = ev.clientX;
        var py = ev.clientY;
        if (ev.touches && ev.touches.length) {
            px = ev.touches[0].clientX;
            py = ev.touches[0].clientY;
        }
        if (px === undefined || py === undefined) return;

        var r = scaled.getBoundingClientRect();
        var d = distPointToRect(px, py, r);
        var ts = nowTs();

        if (d > LEAVE_DIST) {
            panicArm = true;
            return;
        }
        if (!panicArm) return;
        if (d > NEAR_DIST) return;

        panicChargesLeft -= 1;
        panicActiveUntil = ts + rand(PANIC_MS_MIN, PANIC_MS_MAX);
        panicArm = false;
        randomHeading(PANIC_MIN, PANIC_MAX);
    }

    function startFleeing() {
        stage.classList.add('login-fleeing');
        stage.classList.remove('login-settled');
        document.body.classList.remove('login-roach-caught');
        setStagePos(0, 0);
        lastTs = 0;
        frozenUntil = 0;
        panicChargesLeft = 1 + Math.floor(Math.random() * 3);
        panicActiveUntil = 0;
        panicArm = true;
        randomHeading(NORMAL_MIN, NORMAL_MAX);

        var tries = 0;
        function placeInitial() {
            tries += 1;
            if (tries > 200) {
                setStagePos(12, 12);
                if (timerId != null) clearInterval(timerId);
                timerId = window.setInterval(tick, 20);
                return;
            }
            var sz = visualSize();
            var w = sz.w;
            var h = sz.h;
            if (w < 4 || h < 4) {
                window.requestAnimationFrame(placeInitial);
                return;
            }
            var nx = rand(8, Math.max(8, window.innerWidth - w - 8));
            var ny = rand(8, Math.max(8, window.innerHeight - h - 8));
            setStagePos(nx, ny);
            if (timerId != null) {
                clearInterval(timerId);
            }
            timerId = window.setInterval(tick, 20);
        }
        window.requestAnimationFrame(placeInitial);
    }
    function stopFleeing() {
        if (timerId != null) {
            clearInterval(timerId);
            timerId = null;
        }
    }
    function settle() {
        stopFleeing();
        stage.classList.remove('login-fleeing');
        stage.classList.add('login-settled');
        document.body.classList.add('login-roach-caught');
        stage.style.left = '';
        stage.style.top = '';
    }

    document.addEventListener('mousemove', onPointerNear, true);
    document.addEventListener('touchmove', onPointerNear, { capture: true, passive: true });

    stage.addEventListener(
        'click',
        function (e) {
            if (!stage.classList.contains('login-fleeing')) return;
            e.preventDefault();
            e.stopImmediatePropagation();
            settle();
        },
        true
    );
    window.addEventListener('resize', function () {
        if (!stage.classList.contains('login-fleeing')) return;
        placeInBounds();
    });
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', startFleeing);
    } else {
        startFleeing();
    }
})();
