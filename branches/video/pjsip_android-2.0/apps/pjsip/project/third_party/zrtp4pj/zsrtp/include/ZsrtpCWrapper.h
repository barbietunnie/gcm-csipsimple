/*
    This file defines the ZRTP SRTP C-to-C++ wrapper.
    Copyright (C) 2010  Werner Dittmann

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

#ifndef ZSRTPCWRAPPER_H
#define ZSRTPCWRAPPER_H

#include <stdint.h>

/*
 * Keep in synch with CryptoContext.h
 */
#define SrtpAuthenticationNull      0
#define SrtpAuthenticationSha1Hmac  1

#define SrtpEncryptionNull    0
#define SrtpEncryptionAESCM   1
#define SrtpEncryptionAESF8   2

#ifdef __cplusplus
extern "C"
{
#endif
    typedef struct CryptoContext CryptoContext;

    typedef struct zsrtpContext
    {
        CryptoContext* srtp;
        void* userData;
    } ZsrtpContext;

    /**
     * Constructor for an active SRTP cryptographic context.
     *
     * This constructor creates an active SRTP cryptographic context were
     * algorithms are enabled, keys are computed and so on. This SRTP
     * cryptographic context can protect a RTP SSRC stream.
     *
     * @param ssrc
     *    The RTP SSRC that this SRTP cryptographic context protects.
     *
     * @param roc
     *    The initial Roll-Over-Counter according to RFC 3711. These are the
     *    upper 32 bit of the overall 48 bit SRTP packet index. Refer to
     *    chapter 3.2.1 of the RFC.
     *
     * @param keyDerivRate
     *    The key derivation rate defines when to recompute the SRTP session
     *    keys. Refer to chapter 4.3.1 in the RFC.
     *
     * @param ealg
     *    The encryption algorithm to use. Possible values are <code>
     *    SrtpEncryptionNull, SrtpEncryptionAESCM, SrtpEncryptionAESF8
     *    </code>. See chapter 4.1.1 for AESCM (Counter mode) and 4.1.2
     *    for AES F8 mode.
     *
     * @param aalg
     *    The authentication algorithm to use. Possible values are <code>
     *    SrtpEncryptionNull, SrtpAuthenticationSha1Hmac</code>. The only
     *    active algorithm here is SHA1 HMAC, a SHA1 based hashed message
     *    authentication code as defined in RFC 2104.
     *
     * @param masterKey
     *    Pointer to the master key for this SRTP cryptographic context.
     *    Must point to <code>masterKeyLength</code> bytes. Refer to chapter
     *    3.2.1 of the RFC about the role of the master key.
     *
     * @param masterKeyLength
     *    The length in bytes of the master key in bytes. The length must
     *    match the selected encryption algorithm. Because SRTP uses AES
     *    based  encryption only, then master key length may be 16 or 32
     *    bytes (128 or 256 bit master key)
     *
     * @param masterSalt
     *    SRTP uses the master salt to computer the initialization vector
     *    that in turn is input to compute the session key, session
     *    authentication key and the session salt.
     *
     * @param masterSaltLength
     *    The length in bytes of the master salt data in bytes. SRTP uses
     *    AES as encryption algorithm. AES encrypts 16 byte blocks
     *    (independent of the key length). According to RFC3711 the standard
     *    value for the master salt length should be 112 bit (14 bytes).
     *
     * @param ekeyl
     *    The length in bytes of the session encryption key that SRTP shall
     *    compute and use. Usually the same length as for the master key
     *    length. But you may use a different length as well. Be carefull
     *    that the key management mechanisms supports different key lengths.
     *
     * @param akeyl
     *    The length in bytes of the session authentication key. SRTP
     *    computes this key and uses it as input to the authentication
     *    algorithm.
     *    The standard value is 160 bits (20 bytes).
     *
     * @param skeyl
     *    The length in bytes of the session salt. SRTP computes this salt
     *    key and uses it as input during encryption. The length usually
     *    is the same as the master salt length.
     *
     * @param tagLength
     *    The length is bytes of the authentication tag that SRTP appends
     *    to the RTP packet. Refer to chapter 4.2. in the RFC 3711.
     */

    ZsrtpContext* zsrtp_CreateWrapper(uint32_t ssrc, int32_t roc,
                                      int64_t  keyDerivRate,
                                      const  int32_t ealg,
                                      const  int32_t aalg,
                                      uint8_t* masterKey,
                                      int32_t  masterKeyLength,
                                      uint8_t* masterSalt,
                                      int32_t  masterSaltLength,
                                      int32_t  ekeyl,
                                      int32_t  akeyl,
                                      int32_t  skeyl,
                                      int32_t  tagLength);

    void zsrtp_DestroyWrapper (ZsrtpContext* ctx);

    /**
     * Encrypt the RTP payload and compute authentication code.
     *
     * The method requires a ready made RTP packet in the RTP packet data
     * buffer.
     *
     * The method computes an authentication code and appends this code to the
     * buffer and computes a new length. The RTP packet buffer must be large
     * enough to hold this authentication code.
     *
     * @param ctx
     *     The ZsrtpContext
     *
     * @param buffer
     *     Pointer to the data that contains the RTP packet data. SRTP appends
     *     the authentication code to the encrypted RTP packet data.
     *
     * @param length
     *     Length of the RTP data buffer.
     *
     * @param newLength
     *     The new length of the RTP data buffer including authentication code
     */
    int32_t zsrtp_protect(ZsrtpContext* ctx, uint8_t* buffer, int32_t length,
                          int32_t* newLength);

    /**
     * Decrypt the RTP payload and check authentication code.
     *
     * The method requires a SRTP packet in the SRTP packet data
     * buffer.
     *
     * SRTP checks SRTP packet replay, then it computes the authentication
     * code and checks if the authentication code is correct. If the checks
     * are ok then SRTP decrypts the payload data.
     *
     * @param ctx
     *     The ZsrtpContext
     *
     * @param buffer
     *     Pointer to the data that contains the SRTP packet data. SRTP appends
     *     the authentication code to the encrypted RTP packet data.
     *
     * @param length
     *     Length of the RTP data buffer.
     *
     * @param newLength
     *     The new length of the RTP data buffer excluding authentication code
     */
    int32_t zsrtp_unprotect(ZsrtpContext* ctx, uint8_t* buffer, int32_t length,
                            int32_t* newLength);

    /**
     * Derive a new Crypto Context for use with a new SSRC
     *
     * This method stores a new Crypto Context initialized with the data
     * of this crypto context. Replacing the SSRC, Roll-over-Counter, and
     * the key derivation rate the application cab use this Crypto Context
     * to encrypt / decrypt a new stream (Synchronization source) inside
     * one RTP session.
     *
     * Before the application can use this crypto context it must call
     * the <code>deriveSrtpKeys</code> method.
     *
     * @param ssrc
     *     The SSRC for this context
     * @param roc
     *     The Roll-Over-Counter for this context
     * @param keyDerivRate
     *     The key derivation rate for this context
     */
    void zsrtp_newCryptoContextForSSRC(ZsrtpContext* ctx, uint32_t ssrc, 
                                      int32_t roc, int64_t keyDerivRate);

    /**
     * Perform key derivation according to SRTP specification
     *
     * This method computes the session key, session authentication key and the
     * session salt key. This method must be called at least once after the
     * SRTP Cryptograhic context was set up.
     *
     * @param index
     *    The 48 bit SRTP packet index. See the <code>guessIndex</code>
     *    method.
     */                                    
    void zsrtp_deriveSrtpKeys(ZsrtpContext* ctx, uint64_t index);


#ifdef __cplusplus
}
#endif

#endif
