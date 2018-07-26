import {
    clientSessionFactory
} from 'thinbus-srp'
import SRP6CryptoParams from './srp6cryptoparams'
import Net from './net'
import SHA512 from 'crypto-js/sha512'
import Hex from 'crypto-js/enc-hex'
import AES from 'crypto-js/aes'
import Latin1 from 'crypto-js/enc-latin1'
import CryptoJS from 'crypto-js'
import UUID from 'uuid/v4'
import stor from './stor'

export default class Srp {

    static uid = stor("uid");
    static jwt = stor("jwt");
    static key = stor("key");
    static sid = stor("sid");
    static aes = stor("aes");

    static async refreshSession() {
        const nonce = UUID();
        const timestamp = new Date().getTime();
        const aesKey = this.aes();
        if (!aesKey) {
            return;
        }
        const plain = nonce + " " + timestamp;
        const iv = CryptoJS.lib.WordArray.create(Latin1.parse(nonce).words.slice(0, 4));
        const sign = AES.encrypt(plain, Hex.parse(aesKey), {
            iv: iv
        });
        await Net.post("/refresh", {
            nonce: nonce,
            sign: sign.toString(),
            timestamp: timestamp
        })
    }

    static isSignined() {
        return !!this.jwt() && !!this.uid();
    }

    static cleanSession() {
        this.uid(null, 1);
        this.key(null, 1);
        this.jwt(null, 1);
        this.sid(null, 1);
        this.aes(null, 1);
    }

    static saveSession(uid, key, jwt, sid, rememberMe) {
        const aesKey = Hex.stringify(CryptoJS.lib.WordArray.create(SHA512(key).words.slice(0, 8)));
        const exp = rememberMe ? null : 60 * 24;
        this.uid(uid, exp);
        this.key(key, exp);
        this.jwt(jwt, exp);
        this.sid(sid, exp);
        this.aes(aesKey, exp);
    }

    static async doLogin(email, password, rememberMe) {
        const SRP6JavascriptClientSession = clientSessionFactory(SRP6CryptoParams.N_base10, SRP6CryptoParams.g_base10, SRP6CryptoParams.k_base16);
        const srpClient = new SRP6JavascriptClientSession();
        const respCh = Net.postRaw('/challenge', {
            email: email
        });
        srpClient.step1(email, password);
        const responseCh = (await respCh).data;
        let credentials = srpClient.step2(responseCh.salt, responseCh.b);
        const respSi = await Net.postRaw('/authenticate', {
            email: email,
            password: credentials.M1 + ":" + credentials.A
        });
        if (respSi.status === 200) {
            const responseSi = respSi.data;
            srpClient.step3(responseSi.m2);
            const jwt = responseSi.jwt;
            const uid = responseSi.id;
            const key = srpClient.getSessionKey(false);
            const sid = srpClient.getSessionKey(true)
            Srp.saveSession(uid, key, jwt, sid, rememberMe);
        }
        return respSi;
    }

    static async doRegister(email, password, nick, code) {
        const SRP6JavascriptClientSession = clientSessionFactory(SRP6CryptoParams.N_base10, SRP6CryptoParams.g_base10, SRP6CryptoParams.k_base16);
        const srpClient = new SRP6JavascriptClientSession();
        const salt = srpClient.generateRandomSalt();
        const verifier = srpClient.generateVerifier(salt, email, password);
        const respUpP = Net.postRaw('/signup', {
            salt: salt,
            email: email,
            verifier: verifier,
            nick: nick,
            code: code
        });
        srpClient.step1(email, password);
        const respUp = await respUpP;
        const responseUp = respUp.data;
        if (respUp.status !== 202) {
            return {
                respUp: respUp,
                respRe: null
            }
        }
        let credentials = srpClient.step2(responseUp.salt, responseUp.b);
        const respRe = await Net.postRaw('/register', {
            id: responseUp.id,
            password: credentials.M1 + ":" + credentials.A
        });
        if (respRe.status === 201) {
            const responseRe = respRe.data;
            srpClient.step3(responseRe.m2);
            const jwt = responseRe.jwt;
            const uid = responseRe.id;
            const key = srpClient.getSessionKey(false);
            const sid = srpClient.getSessionKey(true)
            Srp.saveSession(uid, key, jwt, sid, true);
        }
        return {
            respUp: respUp,
            respRe: respRe
        }
    }
}