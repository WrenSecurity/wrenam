/**
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015-2016 ForgeRock AS.
 * Portions copyright 2024 Wren Security.
 */

define([
    "jquery",
    "lodash",
    "sinon",
    "org/forgerock/openam/ui/common/util/Promise"
], ($, _, sinon, Promise) => {
    describe("org/forgerock/openam/ui/common/util/Promise", () => {
        describe("#all", () => {
            function whenPassed (value) {
                return () => {
                    it("rejects the promise", (done) => {
                        Promise.all(value).then(() => {
                            done(new Error("Excepted the promise to be rejected"));
                        }, (value) => {
                            expect(value).to.be.an.instanceOf(TypeError);
                            done();
                        });
                    });
                };
            }

            context("when passed null", whenPassed(null));
            context("when passed undefined", whenPassed(undefined));
            context("when passed a number", whenPassed(3));
            context("when passed an object", whenPassed({ a: 1, b: 2 }));

            context("when passed an array", () => {
                context("which is empty", () => {
                    it("resolves with empty array", () => Promise.all([]).then((value) => {
                        expect(value).to.be.an.instanceOf(Array).and.to.be.empty;
                    }));
                });
                context("of 1 promise", () => {
                    let d;
                    let all;
                    let resolvedSpy;
                    let rejectedSpy;

                    beforeEach(() => {
                        d = $.Deferred();
                        resolvedSpy = sinon.spy();
                        rejectedSpy = sinon.spy();
                        all = Promise.all([d.promise()]).then(resolvedSpy, rejectedSpy);
                    });
                    it("returns a pending promise while the passed in promise is pending", () => {
                        expect(resolvedSpy).to.not.be.called;
                        expect(rejectedSpy).to.not.be.called;
                    });
                    it("resolves the returned promise when the passed in promise is resolved", async () => {
                        d.resolve();

                        await all;
                        expect(resolvedSpy).to.be.called;
                        expect(rejectedSpy).to.not.be.called;
                    });
                    it("rejects the returned promise when the passed in promise is rejected", async () => {
                        d.reject(new Error());

                        await all;
                        expect(resolvedSpy).to.not.be.called;
                        expect(rejectedSpy).to.be.called;
                    });
                    it("resolves the promise with an array containing the value of the resolved promise", async () => {
                        d.resolve(1);

                        await all;
                        expect(resolvedSpy).to.be.calledWith([1]);
                    });
                    it("groups multiple resolved values into an array", async () => {
                        d.resolve(1, 2, 3);

                        await all;
                        expect(resolvedSpy).to.be.calledWith([[1, 2, 3]]);
                    });
                });
                context("of 2 promises", () => {
                    let d1;
                    let d2;
                    let all;
                    let resolvedSpy;
                    let rejectedSpy;

                    beforeEach(() => {
                        d1 = $.Deferred();
                        d2 = $.Deferred();
                        resolvedSpy = sinon.spy();
                        rejectedSpy = sinon.spy();
                        all = Promise.all([d1.promise(), d2.promise()]).then(resolvedSpy, rejectedSpy);
                    });
                    it("doesn't resolve the returned promise if neither of the promises are resolved", () => {
                        expect(resolvedSpy).to.not.be.called;
                        expect(rejectedSpy).to.not.be.called;
                    });
                    // eslint-disable-next-line max-len
                    it("doesn't resolve the returned promise if only the first promise is resolved", (done) => {
                        d1.resolve();

                        setTimeout(() => {
                            expect(resolvedSpy).to.not.be.called;
                            expect(rejectedSpy).to.not.be.called;
                            done();
                        }, 1);
                    });
                    // eslint-disable-next-line max-len
                    it("doesn't resolve the returned promise if only the second promise is resolved", (done) => {
                        d2.resolve();

                        setTimeout(() => {
                            expect(resolvedSpy).to.not.be.called;
                            expect(rejectedSpy).to.not.be.called;
                            done();
                        }, 1);
                    });
                    it("resolves the returned promise when both of the promises are resolved", async () => {
                        d1.resolve();
                        d2.resolve();

                        await all;
                        expect(resolvedSpy).to.be.called;
                        expect(rejectedSpy).to.not.be.called;
                    });
                    it("resolves the promise with an array containing the value of the resolved promise", async () => {
                        d1.resolve(1);
                        d2.resolve(2);

                        await all;
                        expect(resolvedSpy).to.be.calledWith([1, 2]);
                    });
                    it("groups multiple resolved values into an array", async () => {
                        d1.resolve(1, 2, 3);
                        d2.resolve(4);

                        await all;
                        expect(resolvedSpy).to.be.calledWith([[1, 2, 3], 4]);
                    });
                    it("rejects the returned promise if the first promise is rejected", async () => {
                        d1.reject();

                        await all;
                        expect(resolvedSpy).to.not.be.called;
                        expect(rejectedSpy).to.be.called;
                    });
                    it("rejects the returned promise if the second promise is rejected", async () => {
                        d2.reject();

                        await all;
                        expect(resolvedSpy).to.not.be.called;
                        expect(rejectedSpy).to.be.called;
                    });
                });
            });
        });
    });
});
