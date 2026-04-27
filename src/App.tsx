/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from 'react';
import { Download, Github, Smartphone } from 'lucide-react';

export default function App() {
  return (
    <div className="min-h-screen bg-neutral-950 text-neutral-50 flex items-center justify-center p-6 font-sans">
      <div className="max-w-2xl w-full bg-neutral-900 border border-neutral-800 rounded-2xl p-8 shadow-2xl">
        <div className="flex items-center space-x-4 mb-6">
          <div className="p-3 bg-emerald-500/20 text-emerald-400 rounded-xl">
            <Smartphone size={32} />
          </div>
          <h1 className="text-3xl font-bold tracking-tight text-white">Android Project Generated</h1>
        </div>
        
        <p className="text-neutral-300 text-lg mb-8 leading-relaxed">
          আপনার রিকোয়েস্ট অনুযায়ী, <strong>OTG Screen Mirror</strong> এর সম্পূর্ণ অ্যান্ড্রয়েড প্রজেক্ট (Kotlin, Manifest, USB & MediaProjection API) ব্যাকএন্ডে তৈরি করা হয়েছে।
        </p>

        <div className="space-y-4 mb-8">
          <div className="p-5 bg-neutral-800/50 border border-neutral-700/50 rounded-xl relative overflow-hidden group">
            <div className="absolute top-0 left-0 w-1 h-full bg-blue-500"></div>
            <h3 className="font-semibold text-blue-400 mb-1 flex items-center gap-2">
              <Github size={18} /> GitHub-এ এক্সপোর্ট করুন
            </h3>
            <p className="text-sm text-neutral-400">
              উপরের ডানদিকের সেটিংস (Settings) বা Share আইকনে ক্লিক করে সরাসরি <strong>Export to GitHub</strong> সিলেক্ট করুন। 
            </p>
          </div>

          <div className="p-5 bg-neutral-800/50 border border-neutral-700/50 rounded-xl relative overflow-hidden group">
            <div className="absolute top-0 left-0 w-1 h-full bg-emerald-500"></div>
            <h3 className="font-semibold text-emerald-400 mb-1 flex items-center gap-2">
              <Download size={18} /> অথবা ZIP ডাউনলোড করুন
            </h3>
            <p className="text-sm text-neutral-400">
              আপনি চাইলে প্রজেক্টটি ZIP ফাইল হিসেবে ডাউনলোড করে Android Studio-তে ওপেন করতে পারেন।
            </p>
          </div>
        </div>

        <div className="mt-8 pt-6 border-t border-neutral-800">
          <h2 className="text-xl font-semibold mb-4 text-white">প্রজেক্টে যা যা যুক্ত করা হয়েছে:</h2>
          <ul className="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm text-neutral-300">
            <li className="flex items-center gap-2"><span className="w-1.5 h-1.5 bg-neutral-500 rounded-full"></span> <code>AndroidManifest.xml</code> (Perms)</li>
            <li className="flex items-center gap-2"><span className="w-1.5 h-1.5 bg-neutral-500 rounded-full"></span> USB Host API Connection</li>
            <li className="flex items-center gap-2"><span className="w-1.5 h-1.5 bg-neutral-500 rounded-full"></span> Android Open Accessory (AOA)</li>
            <li className="flex items-center gap-2"><span className="w-1.5 h-1.5 bg-neutral-500 rounded-full"></span> MediaProjection (Screen Capture)</li>
            <li className="flex items-center gap-2"><span className="w-1.5 h-1.5 bg-neutral-500 rounded-full"></span> MediaCodec (H.264 Encode/Decode)</li>
            <li className="flex items-center gap-2"><span className="w-1.5 h-1.5 bg-neutral-500 rounded-full"></span> Foreground Service</li>
          </ul>
        </div>
      </div>
    </div>
  );
}
